// script.js

/**
 * 공통 HTTP 요청 함수
 * @param {string} method - GET, POST, PUT, DELETE 등
 * @param {string} url - 요청 URL
 * @param {object|null} body - 요청 Body(JSON)
 * @param {object} headers - 추가 헤더
 * @returns {Promise<object>} - JSON 응답 반환
 */
function httpRequest(method, url, body = null, headers = {}) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            ...headers
        },
        credentials: 'include' // 쿠키 자동 전송
    };

    if (body) options.body = JSON.stringify(body);

    return fetch(url, options)
        .then(async response => {
            const text = await response.text();
            const data = text ? JSON.parse(text) : {};
            if (!response.ok) throw { status: response.status, data };
            return data;
        });
}

/**
 * 로그인
 * @param {string} username
 * @param {string} password
 * @returns {Promise<object>}
 */
function login(username, password) {
    return httpRequest('POST', '/api/auth/login', { username, password });
}

/**
 * 로그아웃
 * @returns {Promise<object>}
 */
function logout() {
    return httpRequest('POST', '/api/auth/logout');
}

/**
 * 토큰 갱신
 * @returns {Promise<object>}
 */
function refreshToken() {
    return httpRequest('POST', '/api/auth/refresh');
}

/**
 * 일반 API 호출
 * @param {string} method - GET, POST, PUT, DELETE
 * @param {string} url - 요청 URL
 * @param {object|null} body - 요청 Body
 * @returns {Promise<object>}
 */
function apiRequest(method, url, body = null) {
    return httpRequest(method, url, body);
}

/**
 * JWT 페이로드 디코딩
 * @param {string} token - Base64 URL 인코딩된 JWT Payload
 * @returns {object|null} - 디코딩된 JSON 객체
 */
function decodeJWTPayload(token) {
    try {
        const payload = atob(token.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(payload);
    } catch (e) {
        console.error('Failed to decode JWT payload:', e);
        return null;
    }
}

/**
 * 쿠키 값 읽기
 * @param {string} name - 쿠키 이름
 * @returns {string|null}
 */
function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : null;
}

/**
 * JWT_PAYLOAD 읽기 (프론트에서 필요한 정보)
 * @returns {object|null}
 */
function getJWTPayload() {
    const payload = getCookie('JWT_PAYLOAD');
    if (!payload) return null;
    return decodeJWTPayload(payload);
}
