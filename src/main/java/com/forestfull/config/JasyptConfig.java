package com.forestfull.config;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.StringTokenizer;

/**
 * com.forestfull.config
 *
 * @author vigfoot
 * @version 2025-11-24
 */
@Configuration
public class JasyptConfig {


    @Value("${spring.datasource.password}")
    String test;


    @Bean
    StringEncryptor test(StringEncryptor encryptor, DataSourceProperties dataSourceProperties) {

        String url = dataSourceProperties.getUrl();
        String username = dataSourceProperties.getUsername();
        String password = dataSourceProperties.getPassword();
        for (int i = 0; i < 2; i++) {
            System.out.println("url: ENC(" + encryptor.encrypt(url) + ")");
            System.out.println("username: ENC(" + encryptor.encrypt(username) + ")");
            System.out.println("password: ENC(" + encryptor.encrypt(password) + ")");
        }

        return encryptor;
    }

}
