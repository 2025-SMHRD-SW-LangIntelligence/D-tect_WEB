package com.smhrd.dtect.dto.analysis;

import java.time.Instant;

public record AnalysisStartResponse(String sid, Long analId, Instant startedAt) {}
