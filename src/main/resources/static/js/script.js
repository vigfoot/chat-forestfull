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

    try {
        const response = await fetch(url, options);

        // Access Token 만료 → refresh 시도
        if (response.status === 401 && retry) {
            console.warn("Access Token expired → Refresh Token 시도");

            const refreshed = await refreshTokens();
            if (refreshed) {
                console.warn("재발급 완료 → 원래 요청 재시도");
                return httpRequest(url, method, body, headers, false);
            } else {
                console.warn("Refresh Token 실패 → 로그인 필요");
                redirectToLogin();
            }
        }
        return response;
    } catch (error) {
        console.error(`HTTP 요청 실패: ${error}`);
        throw error;
    }
}

async function refreshTokens() {
    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });

        return response.ok;
    } catch (e) {
        console.error("Refresh 요청 실패:", e);
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
        console.error(`파일 업로드 실패: ${error}`);
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
            console.error('JWT Payload 파싱 실패:', e);
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