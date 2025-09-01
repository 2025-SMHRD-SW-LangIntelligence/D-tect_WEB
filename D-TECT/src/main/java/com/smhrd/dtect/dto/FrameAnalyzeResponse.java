package com.smhrd.dtect.dto;

import java.util.List;

public record FrameAnalyzeResponse(boolean flagged, List<String> labels) {}
