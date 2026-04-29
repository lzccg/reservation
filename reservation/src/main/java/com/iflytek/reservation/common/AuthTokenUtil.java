package com.iflytek.reservation.common;

import jakarta.servlet.http.HttpServletRequest;

public class AuthTokenUtil {
    private static ParsedToken parse(HttpServletRequest request) {
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
        if (token.isBlank()) {
            return null;
        }
        if (token.startsWith("token-")) {
            token = token.substring("token-".length());
        }
        if (token.isBlank()) {
            return null;
        }
        String role = null;
        String idPart = token;
        int idx = token.indexOf('-');
        if (idx > 0 && idx < token.length() - 1) {
            role = token.substring(0, idx).trim();
            idPart = token.substring(idx + 1).trim();
        }
        if (idPart.isBlank()) {
            return null;
        }
        Long id;
        try {
            id = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
        if (role != null && !role.isBlank()) {
            role = role.toLowerCase();
        } else {
            role = null;
        }
        return new ParsedToken(role, id);
    }

    public static Long extractId(HttpServletRequest request) {
        ParsedToken parsed = parse(request);
        return parsed == null ? null : parsed.id;
    }

    public static String extractRole(HttpServletRequest request) {
        ParsedToken parsed = parse(request);
        return parsed == null ? null : parsed.role;
    }

    public static Long extractAdminId(HttpServletRequest request) {
        ParsedToken parsed = parse(request);
        if (parsed == null) {
            return null;
        }
        if (!"admin".equals(parsed.role)) {
            return null;
        }
        return parsed.id;
    }

    public static Long extractStudentId(HttpServletRequest request) {
        ParsedToken parsed = parse(request);
        if (parsed == null) {
            return null;
        }
        if (!"student".equals(parsed.role)) {
            return null;
        }
        return parsed.id;
    }

    public static Long extractCompanyId(HttpServletRequest request) {
        ParsedToken parsed = parse(request);
        if (parsed == null) {
            return null;
        }
        if (!"company".equals(parsed.role)) {
            return null;
        }
        return parsed.id;
    }

    private static class ParsedToken {
        private final String role;
        private final Long id;

        private ParsedToken(String role, Long id) {
            this.role = role;
            this.id = id;
        }
    }
}
