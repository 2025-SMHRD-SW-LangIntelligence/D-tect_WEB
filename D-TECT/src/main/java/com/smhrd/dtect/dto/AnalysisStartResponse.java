// src/main/java/com/smhrd/dtect/dto/AnalysisStartResponse.java
package com.smhrd.dtect.dto;

import java.time.Instant;

public record AnalysisStartResponse(String sid, Long analId, Instant startedAt) {}
