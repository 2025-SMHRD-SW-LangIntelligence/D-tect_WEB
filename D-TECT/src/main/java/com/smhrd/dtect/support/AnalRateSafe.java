package com.smhrd.dtect.support;

import com.smhrd.dtect.entity.AnalRate;

import java.util.Locale;

/** 문자열 → AnalRate 변환 시 예외 방지(NORMAL 폴백) */
public final class AnalRateSafe {
    private AnalRateSafe() {}

    public static AnalRate fromNullable(String rate) {
        if (rate == null) return AnalRate.NORMAL;
        try {
            return AnalRate.valueOf(rate.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return AnalRate.NORMAL;
        }
    }
}
