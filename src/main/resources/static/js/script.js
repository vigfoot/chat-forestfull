let stompClient = null;
let connectedRoomId = null;
const DEFAULT_AVATAR_PATH = '/images/default-avatar.png';

/**
 * UTC ISO 문자열을 받아 클라이언트의 로컬 타임존 기준으로
 * "YYYY-MM-DD HH:mm:ss" 형식의 문자열로 포맷하여 반환합니다.
 * * @param {string} utcIsoString UTC ISO 8601 형식의 시간 문자열
 * @returns {string} 포맷된 로컬 시간 문자열 (예: 2025-12-14 04:16:02)
 */
function getDateTimeFormat(utcIsoString) {
    if (!utcIsoString) return '';
    try {
        const date = new Date(utcIsoString);

        // Date 객체가 유효하지 않은 경우 처리
        if (isNaN(date)) {
            return 'Invalid Date';
        }

        // 🚩 년, 월, 일, 시, 분, 초를 모두 포함하는 포맷팅 옵션
        const options = {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false // 24시간 형식 (HH:mm:ss)
            // timeZoneName 옵션은 제거하여 깔끔하게 시간만 출력
        };

        // toLocaleString을 사용하여 날짜와 시간을 로컬 타임존으로 포맷
        // 예: 'ko-KR' 로케일에서 '2025. 12. 14. 오전 04:16:02' 와 같이 출력될 수 있습니다.
        const formattedDate = date.toLocaleString(navigator.language, options);

        // 🚩 최종적으로 YYYY-MM-DD HH:mm:ss 형식으로 변환하는 후처리 로직 (권장)

        // 1. 날짜와 시간을 분리 (로케일에 따라 분리자 다름: 2025-12-14, 2025. 12. 14., 12/14/2025 등)

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');

        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        const second = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day} ${hour}:${minute}:${second}`;

    } catch (e) {
        console.error("Failed to format date:", e);
        return 'Time N/A';
    }
}

/** WebSocket 연결 */
function connectWebSocket(callback) {
    if (stompClient !== null && stompClient.connected) {
        if (callback) callback();
        return;
    }

    const socket = new SockJS("/ws/chat");
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // 콘솔 로그 제거

    stompClient.connect({}, () => {
        console.log("WebSocket Connected");
        if (callback) callback();
    });
}

// /js/script.js (전역 변수)
let isRefreshing = false;
let failedQueue = []; // 갱신이 완료될 때까지 대기할 요청들을 저장할 배열

// ... (refreshTokens 함수는 그대로 유지) ...

// 실패한 요청을 큐에 추가하고, 갱신이 완료될 때까지 대기
function subscribeTokenRefresh(cb) {
    failedQueue.push(cb);
}

// 갱신이 완료된 후, 큐에 있는 모든 요청 재시도
function onRefreshed() {
    failedQueue.forEach(callback => callback());
    failedQueue = [];
}


async function httpRequest(url, method = 'GET', body = null, headers = {}) {
    // 1. 요청 옵션 설정 (body는 재시도를 위해 함수 스코프 내에서 보존)
    const options = {
        method,
        headers: {...headers},
        credentials: 'include'
    };
    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response = await fetch(url, options);

    if (response.status === 401) {
        // 401 발생 시, 원래 요청을 재구성할 함수 정의
        const retryRequest = async () => {
            // body는 이미 stringify되었으므로 재사용
            const retryResponse = await fetch(url, options);
            return retryResponse;
        };

        // 🚩 2. 토큰 갱신 잠금/큐 처리
        if (!isRefreshing) {
            isRefreshing = true;

            try {
                const refreshed = await refreshTokens();
                if (refreshed) {
                    onRefreshed(); // 대기 중이던 모든 요청 재시도
                    return retryRequest(); // 현재 요청 재시도
                } else {
                    redirectToLogin(); // 갱신 실패 시 즉시 로그인 요청
                    return response; // 401 응답 반환
                }
            } catch (e) {
                // 갱신 중 오류 발생
                redirectToLogin();
                return response;
            } finally {
                isRefreshing = false;
            }

        } else {
            // 갱신이 진행 중이라면, 현재 요청을 큐에 넣고 대기
            return new Promise(resolve => {
                subscribeTokenRefresh(async () => {
                    const retryResponse = await retryRequest();
                    resolve(retryResponse);
                });
            });
        }
    }

    return response;
}

async function refreshTokens() {
    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });
        return response.ok;
    } catch (e) {
        console.error("Refresh request failed:", e); // 🚩 수정
        return false;
    }
}

async function httpFileRequest(url, fileFormData) {
    try {
        return await fetch(url, {
            method: 'POST', // 대부분 업로드는 POST
            body: fileFormData,
            credentials: 'include' // 쿠키 전송
            // headers: Content-Type 지정하지 않음! 브라우저가 자동으로 multipart/form-data 처리
        });
    } catch (error) {
        console.error(`File upload failed: ${error}`); // 🚩 수정
        throw error;
    }
}

/**
 * 편리한 GET 요청
 * @param {string} url
 * @param {Object} headers
 * @returns {Promise<Response>}
 */
async function get(url, headers = {}) {
    return httpRequest(url, 'GET', null, headers);
}

/**
 * 편리한 POST 요청
 * @param {string} url
 * @param {Object} body
 * @param {Object} headers
 * @returns {Promise<Response>}
 */
async function post(url, body = {}, headers = {}) {
    return httpRequest(url, 'POST', body, headers);
}

/**
 * 편리한 PUT 요청
 * @param {string} url
 * @param {Object} body
 * @param {Object} headers
 * @returns {Promise<Response>}
 */
async function put(url, body = {}, headers = {}) {
    return httpRequest(url, 'PUT', body, headers);
}

/**
 * 편리한 DELETE 요청
 * @param {string} url
 * @param {Object} body
 * @param {Object} headers
 * @returns {Promise<Response>}
 */
async function del(url, body = null, headers = {}) {
    return httpRequest(url, 'DELETE', body, headers);
}

/**
 * JWT Payload 추출 (base64 → JSON)
 * - 프론트에서 JWT_PAYLOAD 쿠키 읽고 디코딩 가능
 * @param {string} cookieName
 * @returns {Object|null}
 */
function getJwtPayload(cookieName = 'JWT_PAYLOAD') {
    let cookie = document.cookie;
    const match = cookie.match(new RegExp('(^| )' + cookieName + '=([^;]+)'));
    if (match) {
        try {
            const payloadBase64 = match[2];
            const payloadJson = atob(payloadBase64);
            return JSON.parse(payloadJson);
        } catch (e) {
            console.error('JWT Payload parsing failed:', e); // 🚩 수정
            return null;
        }
    }
    return null;
}

/**
 * Global function to display a stacked alert (Toast-like).
 * @param {string} message Message to display
 * @param {string} type Bootstrap alert class (primary, success, danger, warning, etc.)
 * @param {number} duration Time in milliseconds before auto-hide
 */
function showAlert(message, type = 'warning', duration = 1000) {
    const container = document.getElementById('alert-container');
    if (!container) {
        console.warn("Alert container element not found (#alert-container).");
        return;
    }

    // 🚩 1. 새로운 Alert 요소 동적 생성
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.setAttribute('role', 'alert');
    // Stacked 알림을 위해 너비를 제한하고 마진을 줍니다.
    alertDiv.style.width = '95vw';
    alertDiv.style.marginBottom = '10px';

    // 🚩 2. Alert 내용 구성
    alertDiv.innerHTML = `
        <span class="d-block" style="word-break: break-word;">${message}</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;

    // 컨테이너에 Alert 추가 (새 Alert가 가장 위에 쌓이도록)
    container.prepend(alertDiv); // prepend를 사용하여 위에서 아래로 쌓이도록 함 (Top-down stack)

    // 🚩 3. 닫기 이벤트 리스너 부착
    // Bootstrap의 `closed.bs.alert` 이벤트는 애니메이션이 끝난 후 발생하며,
    // 이 이벤트 핸들러 내에서 DOM 요소를 안전하게 제거합니다.
    alertDiv.addEventListener('closed.bs.alert', function () {
        alertDiv.remove(); // Alert 요소가 완전히 닫힌 후 DOM에서 제거
    });

    // 🚩 4. 자동 닫기 타이머 설정
    if (duration > 0) {
        setTimeout(() => {
            // 이 시점에 Alert 요소가 수동으로 닫히고 제거되었을 수 있습니다.
            // Bootstrap의 Alert 인스턴스를 가져와서 닫기를 시도합니다.
            const bsAlert = bootstrap.Alert.getInstance(alertDiv);

            if (bsAlert) {
                // 인스턴스가 존재하면 안전하게 닫기 명령을 내립니다.
                // 닫기 명령을 내리면 위에서 정의한 'closed.bs.alert' 리스너가 최종적으로 remove()를 처리합니다.
                bsAlert.close();
            } else if (container.contains(alertDiv)) {
                // 인스턴스는 없지만 DOM에는 남아있다면, 직접 제거 (비정상적인 상황 방지)
                alertDiv.remove();
            }
        }, duration);
    }
}

// 전역 변수: 모달이 열리기 전 마지막으로 포커스된 요소를 저장합니다.
let lastFocusedElementBeforeModal = null;

// ... (다른 전역 함수 생략)

/**
 * Global function to display the common modal. (수정됨)
 *
 * @param {string} title 모달 제목
 * @param {string} bodyHtml 모달 본문 HTML
 * @param {function|null} confirmAction 'Confirm' 버튼 클릭 시 실행할 함수.
 * @param {Object} options 모달 동작 관련 옵션 객체
 * @param {boolean} options.isStatic 모달을 ESC 키나 배경 클릭으로 닫지 못하게 할지 여부
 * @param {boolean} options.showClose Action이 있을 때도 Close 버튼을 표시할지 여부
 * @param {boolean} options.center 모달을 수직 중앙에 배치할지 여부
 * @param {string} options.customModalClass 모달 크기 조정을 위한 추가 클래스
 */
function showModal(title, bodyHtml, confirmAction = null, options = {}) {
    // 1. 기본 옵션 설정 및 병합
    const defaultOptions = {
        isStatic: true,
        showClose: false,
        center: false,
        customModalClass: ''
    };

    let finalOptions = {...defaultOptions, ...options};

    // 🚩 2. 포커스 저장: 현재 포커스된 요소를 저장
    lastFocusedElementBeforeModal = document.activeElement;

    // 3. Static 비활성화 조건 확인 및 적용 (변경 없음)
    const onlyCloseButton = !confirmAction && !finalOptions.showClose;
    const bothButtons = confirmAction && finalOptions.showClose;

    if (onlyCloseButton || bothButtons) {
        if (options.isStatic !== true) {
            finalOptions.isStatic = false;
        }
    }

    // --- 4. DOM 요소 및 인스턴스 준비 (변경 없음) ---
    const modalElement = document.getElementById('commonModal');
    const dialogElement = modalElement?.querySelector('.modal-dialog');

    if (!modalElement || !dialogElement) {
        console.error("Modal element 'commonModal' or '.modal-dialog' not found.");
        return;
    }

    document.getElementById('commonModalLabel').textContent = title;
    document.getElementById('commonModalBody').innerHTML = bodyHtml;

    // 클래스 적용 (변경 없음)
    dialogElement.classList.toggle('modal-dialog-centered', finalOptions.center);
    dialogElement.className = dialogElement.className.replace(/\bmodal-(sm|lg|xl)\b/g, '');
    if (finalOptions.customModalClass) {
        dialogElement.classList.add(finalOptions.customModalClass);
    }

    // 이전 인스턴스 정리 및 새 인스턴스 생성 (변경 없음)
    let modalInstance = bootstrap.Modal.getInstance(modalElement);
    if (modalInstance) {
        modalInstance.dispose();
    }

    const bootstrapOptions = finalOptions.isStatic
        ? {backdrop: 'static', keyboard: false}
        : {};

    modalInstance = new bootstrap.Modal(modalElement, bootstrapOptions);

    const footer = document.getElementById('commonModalFooter');
    footer.innerHTML = '';

    // --- 5. 모달 닫힘 이벤트 리스너 부착 (수정) ---

    // 🚩 [A] hide.bs.modal: 모달이 사라지기 직전에 포커스 복원 (경고 방지 목적)
    function restoreFocus(event) {
        if (lastFocusedElementBeforeModal && lastFocusedElementBeforeModal.focus) {
            lastFocusedElementBeforeModal.focus();
        }
    }

    // 🚩 [B] hidden.bs.modal: 모달이 완전히 사라진 후 변수 초기화 및 리스너 제거
    function cleanupAfterModalHidden() {
        lastFocusedElementBeforeModal = null;
        document.getElementById('commonModalLabel').textContent = '';
        document.getElementById('commonModalBody').innerHTML = '';
        // 리스너 제거
        modalElement.removeEventListener('hide.bs.modal', restoreFocus);
        modalElement.removeEventListener('hidden.bs.modal', cleanupAfterModalHidden);
    }

    // 리스너 부착
    modalElement.addEventListener('hide.bs.modal', restoreFocus);
    modalElement.addEventListener('hidden.bs.modal', cleanupAfterModalHidden);

    // --- 6. 버튼 생성 헬퍼 함수 (수정) ---
    function createButton(action, classname, text) {
        const btn = document.createElement('button');
        btn.setAttribute('type', 'button');
        btn.className = classname;
        btn.textContent = text;

        // 🚩 [핵심 수정 1]: data-bs-dismiss="modal"을 기본적으로 제거. 모든 닫기 동작은 JS가 관리합니다.

        btn.addEventListener('click', () => {
            // 🚩 [핵심 수정 2]: 클릭 후 즉시 버튼에서 포커스를 제거합니다.
            btn.blur();

            // 모달 닫기 명령
            modalInstance.hide();

            // Confirm 액션 실행 (action이 null이 아닐 경우)
            if (action) {
                action();
            }
        });

        footer.appendChild(btn);
    }

    // --- 7. 버튼 생성 로직 (변경 없음) ---
    if (confirmAction) {
        // A. Confirm 버튼
        createButton(confirmAction, 'btn btn-primary', 'Confirm');

        // B-1. Close 버튼
        if (finalOptions.showClose) {
            createButton(null, 'btn btn-secondary', 'Close');
        }
    } else {
        // B-2. Close 버튼
        createButton(null, 'btn btn-secondary', 'Close');
    }

    // --- 8. 모달 표시 (변경 없음) ---
    modalInstance.show();
}

function redirectIndexPage() {
    window.location.href = '/';
}

async function handleLogout() {
    try {
        // POST request to the logout endpoint
        const response = await post('/api/auth/logout', null);
        if (response.ok) {
            showModal('Log Out', 'Logged out successfully.', redirectIndexPage);
        } else {
            showModal('Log Out', 'Error occurred during logout.');
        }
    } catch (err) {
        console.error(err);
        showModal('Log Out', 'Communication error with the server.');
    }
}

// ------------------------------------------------
// 6. Media Modal Logic (새로 추가)
// ------------------------------------------------

/**
 * 이미지/비디오를 큰 화면 모달에 표시하고 재생합니다.
 * @param {string} url - 파일의 웹 접근 URL
 * @param {string} type - 'image' 또는 'video'
 */
function showMediaModal(url, type) {
    let mediaHtml = '';
    let title = '';

    if (type === 'image') {
        title = "Image Viewer";
        // 큰 이미지 표시 (클릭 이벤트 제거)
        mediaHtml = `<img src="${url}" alt="Image" style="max-width: 100%; max-height: 80vh; display: block; margin: auto;">`;
    } else if (type === 'video') {
        title = "Video Player";
        // 비디오 재생 (controls 추가, 자동 재생)
        mediaHtml = `<video src="${url}" controls autoplay style="max-width: 100%; max-height: 80vh; display: block; margin: auto;"></video>`;
    } else {
        return;
    }

    // isStatic: true (모달 바깥 클릭으로 닫히지 않음)
    showModal(
        title,
        mediaHtml,
        null, // Confirm 버튼 없음
        {
            isStatic: true,
            showClose: true,
            center: true,
            customModalClass: 'modal-xl' // 모달 크기를 키워서 미디어를 더 크게 표시
        }
    );
}