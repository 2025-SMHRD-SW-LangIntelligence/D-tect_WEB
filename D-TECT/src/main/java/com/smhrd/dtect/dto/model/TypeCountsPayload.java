package com.smhrd.dtect.dto.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smhrd.dtect.entity.analysis.AnalRate;
import com.smhrd.dtect.entity.field.FieldName;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TypeCountsPayload(
        Long userId,
        String sid,
        Period period,
        Map<FieldName, Integer> typeCounts,
        AnalRate analRate
) {
    public record Period(Instant startedAt, Instant endedAt) {}
}
