package com.forestfull.common;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommonResponse {
    private boolean success;
    private String message;
    private Object data;

    public static CommonResponse ok(){
        return CommonResponse.builder().success(true).build();
    }

    public static CommonResponse fail(String message){
        return CommonResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
