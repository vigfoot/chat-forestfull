package com.forestfull;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class EncryptionTest {

    @Autowired
    private StringEncryptor encryptor;

    @Test
    void encrypt() {
        String[] keyValue = {
                "client-key", ""
                , "client-secret", ""
                , "redirect-uri", ""
                , "scope", ""
        };

        for (int i = 0; i < keyValue.length; i = i + 2) {
            System.out.println(keyValue[i] + ": " + keyValue[i + 1]);
        }

        for (int i = 0; i < keyValue.length; i = i + 2) {
            System.out.println(keyValue[i] + ": ENC(" + encryptor.encrypt(keyValue[i + 1]) + ")");
        }
    }
}
