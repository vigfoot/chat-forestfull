package com.forestfull;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EncryptionTest {

    @Autowired
    @Qualifier("jasyptStringEncryptor")
    private StringEncryptor encryptor;

    @Test
    void encrypt() {
        String[] keyValue = {""
                , "client-key", ""

        };

        String[] encodedKeyValue = new String[keyValue.length];

        for (int i = 1; i < keyValue.length; i = i + 2) {
            System.out.println(keyValue[i] + ": " + keyValue[i + 1]);
        }

        System.out.println("============================encrypt=================================");

        for (int i = 1; i < keyValue.length; i = i + 2) {
            encodedKeyValue[i] = keyValue[i];
            encodedKeyValue[i + 1] = encryptor.encrypt(keyValue[i + 1]);

            System.out.println(encodedKeyValue[i] + ": ENC(" + encodedKeyValue[i + 1] + ")");
        }

        System.out.println("============================decrypt=================================");

        for (int i = 1; i < keyValue.length; i = i + 2) {
            System.out.println(encodedKeyValue[i] + ": " + encryptor.decrypt(encodedKeyValue[i + 1]));
        }
    }
}
