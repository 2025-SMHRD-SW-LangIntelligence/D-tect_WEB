package com.smhrd.dtect.support;

import com.smhrd.dtect.entity.analysis.AnalRate;

import java.util.Locale;
import java.util.Map;

public final class AnalRateSafe {
    private AnalRateSafe() {}

    // 동의어/한글 라벨 매핑
    private static final Map<String, AnalRate> ALIASES = Map.ofEntries(
            Map.entry("NORMAL", AnalRate.NORMAL),
            Map.entry("DEFAULT", AnalRate.NORMAL),
            Map.entry("정상", AnalRate.NORMAL),
            Map.entry("보통", AnalRate.NORMAL),

            Map.entry("WARNING", AnalRate.WARNING),
            Map.entry("WARN", AnalRate.WARNING),
            Map.entry("경고", AnalRate.WARNING),

            Map.entry("DANGER", AnalRate.DANGER),
            Map.entry("RISK", AnalRate.DANGER),
            Map.entry("위험", AnalRate.DANGER)
    );

    /** null/빈값/미매칭 → NORMAL */
    public static AnalRate fromNullable(String rate) {
        return fromOrDefault(rate, AnalRate.NORMAL);
    }

    /** null/빈값/미매칭 → 지정한 fallback */
    public static AnalRate fromOrDefault(String rate, AnalRate fallback) {
        if (rate == null) return fallback;
        String key = rate.trim();
        if (key.isEmpty()) return fallback;
        // 한글/영문 혼용 대응
        AnalRate mapped = ALIASES.get(key);
        if (mapped != null) return mapped;
        try {
            return AnalRate.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
