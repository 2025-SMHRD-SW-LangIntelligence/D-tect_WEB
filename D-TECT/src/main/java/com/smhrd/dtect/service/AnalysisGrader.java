package com.smhrd.dtect.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smhrd.dtect.dto.ModelResultDto;
import com.smhrd.dtect.entity.AnalRate;

public final class AnalysisGrader {

    private AnalysisGrader() {}

    public static AnalRate grade(List<ModelResultDto> items) {
        if (items == null || items.isEmpty()) return AnalRate.NORMAL;

        Map<String, Integer> counts = new HashMap<>();
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;
        boolean dangerHit = false;

        for (ModelResultDto r : items) {
            if (r == null) continue;
            var c = r.classification();
            String label = c != null ? c.label() : "UNKNOWN";
            int cnt = c != null ? c.count() : 0;
            counts.merge(label, cnt, Integer::sum);

            // score는 문자열로 온다고 했으니 안전 파싱
            try {
                if (r.score() != null) {
                    scoreSum = scoreSum.add(new BigDecimal(r.score()));
                    scoreCnt++;
                }
            } catch (Exception ignore) { /* no-op */ }

            // VIOLENCE 고득점 즉시 DANGER 후보
            try {
                if ("VIOLENCE".equalsIgnoreCase(label) && r.score() != null) {
                    BigDecimal s = new BigDecimal(r.score());
                    if (s.compareTo(new BigDecimal("0.85")) >= 0) {
                        dangerHit = true;
                    }
                }
            } catch (Exception ignore) { /* no-op */ }
        }

        int totalCount = counts.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal avgScore = (scoreCnt > 0) ? scoreSum.divide(BigDecimal.valueOf(scoreCnt), 4, BigDecimal.ROUND_HALF_UP)
                                             : BigDecimal.ZERO;

        if (dangerHit) return AnalRate.DANGER;
        if (totalCount >= 3 && avgScore.compareTo(new BigDecimal("0.80")) >= 0) return AnalRate.DANGER;
        if (totalCount >= 1 && avgScore.compareTo(new BigDecimal("0.60")) >= 0) return AnalRate.WARNING;
        return AnalRate.NORMAL;
    }
}
