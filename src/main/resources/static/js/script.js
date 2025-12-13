let stompClient = null;
let connectedRoomId = null;

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
        headers: { ...headers },
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
 * Global function to display the top alert.
 * @param {string} message Message to display
 * @param {string} type Bootstrap alert class (primary, success, danger, warning, etc.)
 */
function showAlert(message, type = 'warning') {
    const alertArea = document.getElementById('top-alert-area');
    const alertMessage = document.getElementById('top-alert-message');

    if (!alertArea || !alertMessage) {
        console.warn("Alert DOM elements not found (top-alert-area or top-alert-message).");
        return;
    }

    alertArea.className = `alert alert-${type} alert-dismissible fade show`;
    alertMessage.textContent = message;
    alertArea.classList.remove('d-none'); // Show alert

    // (Optional) Auto-hide after 5 seconds
    setTimeout(() => {
        const bsAlert = bootstrap.Alert.getOrCreateInstance(alertArea);
        bsAlert.close();
    }, 5000);
}

/**
 * Global function to display the common modal.
 *
 * @param {string} title 모달 제목
 * @param {string} bodyHtml 모달 본문 HTML
 * @param {function|null} confirmAction 'Confirm' 버튼 클릭 시 실행할 함수. (null이면 Action 버튼 미표시)
 * @param {Object} options 모달 동작 관련 옵션 객체
 * @param {boolean} options.isStatic 모달을 ESC 키나 배경 클릭으로 닫지 못하게 할지 여부 (기본값: true)
 * @param {boolean} options.showClose Action이 있을 때도 Close 버튼을 표시할지 여부 (기본값: false)
 */
function showModal(title, bodyHtml, confirmAction = null, options = {}) {
    // 1. 기본 옵션 설정 (isStatic의 기본값을 true로 변경)
    const defaultOptions = {
        isStatic: true, // 🚩 기본값을 true로 설정
        showClose: false
    };

    let finalOptions = { ...defaultOptions, ...options };

    // 2. 🚩 핵심 로직: Static 비활성화 조건 확인 및 적용
    const onlyCloseButton = !confirmAction && !finalOptions.showClose;
    const bothButtons = confirmAction && finalOptions.showClose;

    // 취소만 있거나 (onlyCloseButton), 액션과 취소가 모두 있을 때 (bothButtons) static을 false로 설정
    if (onlyCloseButton || bothButtons) {
        // 단, 사용자가 options에서 isStatic을 명시적으로 true로 설정했다면 덮어쓰지 않습니다.
        if (options.isStatic !== true) {
            finalOptions.isStatic = false;
        }
    }

    // --- 3. DOM 요소 및 인스턴스 준비 (이전과 동일) ---

    const modalElement = document.getElementById('commonModal');

    if (!modalElement) {
        console.error("Modal element 'commonModal' not found.");
        return;
    }

    document.getElementById('commonModalLabel').textContent = title;
    document.getElementById('commonModalBody').innerHTML = bodyHtml;

    const footer = document.getElementById('commonModalFooter');
    footer.innerHTML = '';

    const bootstrapOptions = finalOptions.isStatic
        ? { backdrop: 'static', keyboard: false }
        : {};

    const modalInstance = new bootstrap.Modal(modalElement, bootstrapOptions);

    // --- 4. 버튼 생성 헬퍼 함수 ---

    function createButton(action, classname, text) {
        const btn = document.createElement('button');
        btn.setAttribute('type', 'button');
        btn.className = classname;
        btn.textContent = text;

        btn.setAttribute('data-bs-dismiss', 'modal');

        if (action) {
            btn.addEventListener('click', () => {
                action();
                modalInstance.hide();
            });
        }

        footer.appendChild(btn);
    }

    // --- 5. 버튼 생성 로직 ---

    if (confirmAction) {
        // A. Confirm 버튼 (Action이 있을 때)
        createButton(confirmAction, 'btn btn-primary', 'Confirm');

        // B-1. Close 버튼 (Action이 있고, showClose 옵션이 true일 때)
        if (finalOptions.showClose) {
            createButton(null, 'btn btn-secondary', 'Close');
        }
    } else {
        // B-2. Close 버튼 (Action이 없을 때 자동으로 생성)
        createButton(null, 'btn btn-secondary', 'Close');
    }

    // --- 6. 모달 표시 ---
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