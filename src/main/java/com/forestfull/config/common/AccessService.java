package com.forestfull.config.common;

import com.forestfull.oauth.tiktok.UserAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * com.forestfull.config.common
 *
 * @author vigfoot
 * @version 2025-11-24
 */
@Service
public class AccessService {

    void test(){
        WebClient.builder()
                .baseUrl(UserAccess.END_POINT)
                .build()
                .method(UserAccess.METHOD);



    }

}
