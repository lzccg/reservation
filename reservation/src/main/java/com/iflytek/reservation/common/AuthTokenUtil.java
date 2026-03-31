package com.iflytek.reservation.common;

import jakarta.servlet.http.HttpServletRequest;

public class AuthTokenUtil {
    public static Long extractId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String token = header;
        if (header.startsWith("Bearer ")) {
            token = header.substring("Bearer ".length()).trim();
        }
        if (token.startsWith("token-")) {
            token = token.substring("token-".length());
        }
        if (token.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

