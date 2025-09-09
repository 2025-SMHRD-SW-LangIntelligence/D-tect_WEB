package com.smhrd.dtect.service.analysis;

import com.smhrd.dtect.dto.model.LabelCount;
import com.smhrd.dtect.dto.model.ModelMessage;
import com.smhrd.dtect.entity.analysis.AnalRate;

import java.math.BigDecimal;
import java.util.List;

public final class AnalysisGrader {
    private AnalysisGrader(){}

    public static AnalRate grade(List<ModelMessage> items) {
        if (items == null || items.isEmpty()) return AnalRate.NORMAL;

        int violenceCnt = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;
        boolean dangerHit = false;

        for (ModelMessage m : items) {
            if (m == null) continue;

            // score 평균
            try {
                if (m.getScore() != null) {
                    scoreSum = scoreSum.add(new BigDecimal(m.getScore()));
                    scoreCnt++;
                }
            } catch (Exception ignore) {}

            // 라벨 카운트 집계
            List<LabelCount> list = m.getClassification();
            if (list != null) {
                for (LabelCount lc : list) {
                    if (lc == null) continue;
                    if ("VIOLENCE".equalsIgnoreCase(lc.getLabel())) {
                        violenceCnt += lc.getCount();
                        try {
                            if (m.getScore() != null &&
                                new BigDecimal(m.getScore()).compareTo(new BigDecimal("0.85")) >= 0) {
                                dangerHit = true;
                            }
                        } catch (Exception ignore) {}
                    }
                }
            }
        }

        BigDecimal avgScore = (scoreCnt > 0) ? scoreSum.divide(BigDecimal.valueOf(scoreCnt), 4, BigDecimal.ROUND_HALF_UP)
                                             : BigDecimal.ZERO;

        if (dangerHit) return AnalRate.DANGER;
        if (violenceCnt >= 3 && avgScore.compareTo(new BigDecimal("0.80")) >= 0) return AnalRate.DANGER;
        if (violenceCnt >= 1 && avgScore.compareTo(new BigDecimal("0.60")) >= 0) return AnalRate.WARNING;
        return AnalRate.NORMAL;
    }
}
