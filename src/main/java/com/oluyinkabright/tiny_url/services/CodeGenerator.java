package com.oluyinkabright.tiny_url.services;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CodeGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final int MAX_RETRIES = 5;
    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        return generateCode(DEFAULT_CODE_LENGTH);
    }

    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
        }
        return sb.toString();
    }

    public String generateUniqueCode(CodeChecker codeChecker) {
        return generateUniqueCode(codeChecker, DEFAULT_CODE_LENGTH, MAX_RETRIES);
    }

    public String generateUniqueCode(CodeChecker codeChecker, int length, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            String code = generateCode(length);
            if (!codeChecker.isCodeExists(code)) {
                return code;
            }
            attempts++;
        }
        throw new IllegalStateException("Unable to generate unique code after " + maxRetries + " attempts");
    }

    @FunctionalInterface
    public interface CodeChecker {
        boolean isCodeExists(String code);
    }
}