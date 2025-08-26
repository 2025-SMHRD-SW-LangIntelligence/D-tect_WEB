package com.smhrd.dtect.dto;

import java.util.List;
import java.util.Map;

public record BotMessageResponse(
	    String reply,
	    List<String> citations,
	    Map<String, Object> meta
	) {}
