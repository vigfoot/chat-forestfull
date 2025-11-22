package com.forestfull.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

/**
 * com.forestfull.common
 *
 * @author vigfoot
 * @version 2025-11-22
 */
@Configuration
public class CommonBeans {

    @Bean
    HttpClient httpClient() {
        return HttpClient.create();
    }


}
