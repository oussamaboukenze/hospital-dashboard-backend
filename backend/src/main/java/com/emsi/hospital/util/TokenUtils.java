package com.emsi.hospital.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenUtils {

    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"(\\d+)\"");

    private TokenUtils() {}

    public static Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String token = authHeader.substring(7).trim();
            String json = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).trim();
            Matcher m = SUB_PATTERN.matcher(json);
            return m.find() ? Long.parseLong(m.group(1)) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
