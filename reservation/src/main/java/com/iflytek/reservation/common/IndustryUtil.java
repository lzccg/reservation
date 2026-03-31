package com.iflytek.reservation.common;

import java.util.Map;

public class IndustryUtil {
    private static final Map<String, String> CANONICAL = Map.of(
            "计算机/互联网", "计算机/互联网",
            "internet", "计算机/互联网",
            "金融/保险", "金融/保险",
            "finance", "金融/保险",
            "教育/培训", "教育/培训",
            "education", "教育/培训",
            "制造/工业", "制造/工业",
            "manufacture", "制造/工业",
            "其他", "其他",
            "other", "其他"
    );

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isBlank()) {
            return null;
        }
        String key = v.toLowerCase();
        String mapped = CANONICAL.get(key);
        if (mapped != null) {
            return mapped;
        }
        mapped = CANONICAL.get(v);
        return mapped;
    }
}

