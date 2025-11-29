package com.forestfull.common;


import lombok.Builder;

@Builder
public class ResponseException {
    private boolean success;
    private String message;
    private Object data;

    public static ResponseException ok(){
        return ResponseException.builder().success(true).build();
    }

    public static ResponseException fail(String message){
        return ResponseException.builder()
                .success(false)
                .message(message)
                .build();
    }
}
