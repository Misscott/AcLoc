package com.example.acloc.utility;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KeyGeneratorUtils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_STRING_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a unique key using timestamp + random string
     * @return Unique string key
     */
    public static String uniqueKeyGenerator() {
        // Get current timestamp in yyyyMMddHHmmssSSS format
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());

        // Generate random alphanumeric string
        StringBuilder randomStr = new StringBuilder(RANDOM_STRING_LENGTH);
        for (int i = 0; i < RANDOM_STRING_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            randomStr.append(CHARACTERS.charAt(index));
        }

        // Combine timestamp and random string
        return timeStamp + "_" + randomStr.toString();
    }
}