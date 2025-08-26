package com.smhrd.dtect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smhrd.dtect.dto.LabelCount;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.entity.AnalRate;

/**
 * 간단한 등급 산정 로직.
 * - score 평균 & 라벨 카운트 기반으로 NORMAL/WARNING/DANGER 판정
 */
public final class AnalysisGrader {

    private AnalysisGrader() {}

    public static AnalRate grade(List<ModelMessage> items) {
        if (items == null || items.isEmpty()) return AnalRate.NORMAL;

        Map<String, Integer> counts = new HashMap<>();
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;
        boolean dangerHit = false;

        for (ModelMessage m : items) {
            // score 평균
            try {
                if (m.getScore() != null) {
                    BigDecimal s = new BigDecimal(m.getScore());
                    scoreSum = scoreSum.add(s);
                    scoreCnt++;
                    if (s.compareTo(new BigDecimal("0.90")) >= 0) {
                        dangerHit = true;
                    }
                }
            } catch (Exception ignore) { /* no-op */ }

            // 라벨 카운팅
            if (m.getClassification() != null) {
                for (LabelCount lc : m.getClassification()) {
                    if (lc == null || lc.getLabel() == null) continue;
                    counts.merge(lc.getLabel(), lc.getCount(), Integer::sum);
                    if ("BULLYING".equalsIgnoreCase(lc.getLabel()) && lc.getCount() >= 1) {
                        dangerHit = true;
                    }
                }
            }
        }

        int totalCount = counts.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal avgScore = (scoreCnt > 0)
                ? scoreSum.divide(BigDecimal.valueOf(scoreCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (dangerHit) return AnalRate.DANGER;
        if (totalCount >= 3 && avgScore.compareTo(new BigDecimal("0.80")) >= 0) return AnalRate.DANGER;
        if (totalCount >= 1 && avgScore.compareTo(new BigDecimal("0.60")) >= 0) return AnalRate.WARNING;
        return AnalRate.NORMAL;
    }
}
