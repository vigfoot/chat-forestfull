package com.forestfull.oauth.tiktok;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * com.forestfull.oauth
 *
 * @author vigfoot
 * @version 2025-11-22
 */
public class UserAccess {

    public static final HttpMethod METHOD = HttpMethod.POST;
    public static final String END_POINT = "https://open.tiktokapis.com/v2/oauth/token/";

    public static class Request {
        private String client_key;
        private String client_secret;
        private String code;
        private String grant_type;
        private String redirect_uri;
        private String code_verifier;
    }

    public static class Response {
        private String open_id;
        private String scope;
        private String access_token;
        private Integer expires_in;
        private String refresh_token;
        private Integer refresh_expires_in;
        private String token_type;

    }
}