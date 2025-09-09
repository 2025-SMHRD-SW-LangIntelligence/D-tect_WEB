package com.smhrd.dtect.dto.analysis;

import java.util.List;

public record FrameAnalyzeResponse(boolean flagged, List<String> labels) {}
