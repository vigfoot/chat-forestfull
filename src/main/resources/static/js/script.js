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

/**
 * 공통 HTTP 요청 함수
 * - 모든 요청에 쿠키 포함
 * - GET / POST / PUT / DELETE 지원
 * - JSON body 자동 처리
 * - Promise 반환
 */
async function httpRequest(url, method = 'GET', body = null, headers = {}, retry = true) {
    const options = {
        method,
        headers: { ...headers },
        credentials: 'include'
    };

    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    return fetch(url, options);
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

function redirectToLogin() {
    deleteCookie('JWT');
    deleteCookie('JWT_PAYLOAD');
    deleteCookie('REFRESH');
    window.location.href = '/';
}

/**
 * 쿠키 삭제
 * @param {string} name
 */
function deleteCookie(name) {
    document.cookie = name + '=; Max-Age=0; path=/; SameSite=None; Secure';
}

/**
 * 쿠키 읽기
 * @param {string} name
 * @returns {string|null}
 */
function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

// src/main/resources/static/js/script.js (추가/수정 필요)

// ... (기존의 post, get, getJwtPayload 함수 등) ...

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
 * @param {string} title Modal title
 * @param {string} bodyHtml Modal content (HTML string)
 * @param {function|null} confirmAction Function to execute when the primary confirmation button is clicked.
 */
function showModal(title, bodyHtml, confirmAction = null) {
    const modalEl = document.getElementById('commonModal');
    if (!modalEl) {
        console.error("Modal element 'commonModal' not found. Ensure footer.html is included.");
        return;
    }

    document.getElementById('commonModalLabel').textContent = title;
    document.getElementById('commonModalBody').innerHTML = bodyHtml;

    const footer = document.getElementById('commonModalFooter');
    footer.innerHTML = '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>';

    if (confirmAction) {
        const confirmBtn = document.createElement('button');
        confirmBtn.setAttribute('type', 'button');
        confirmBtn.className = 'btn btn-primary';
        confirmBtn.textContent = 'Confirm';

        // Clone the button to remove existing event listeners safely
        const newConfirmBtn = confirmBtn.cloneNode(true);
        newConfirmBtn.addEventListener('click', () => {
            confirmAction();
            // Hide modal instance safely
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) {
                modalInstance.hide();
            }
        });

        footer.appendChild(newConfirmBtn);
    }

    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}
async function handleLogout() {
    try {
        // POST request to the logout endpoint
        const response = await post('/api/auth/logout', null);
        if (response.ok) {
            // Remove JWT/local storage items if necessary (assuming handled by common script/backend)
            alert('Logged out successfully.');
            // Redirect to home or login page
            window.location.href = '/';
        } else {
            alert('Error occurred during logout.');
        }
    } catch (err) {
        console.error(err);
        alert('Communication error with the server.');
    }
}