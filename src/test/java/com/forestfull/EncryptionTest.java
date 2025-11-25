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
        String encrypted = encryptor.encrypt("blarblar");
        System.out.println("Encrypted: " + encrypted);
    }
}
