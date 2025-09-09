package com.smhrd.dtect.dto.bot;

import java.util.Map;

public record BotMessageRequest(
	    String sessionId,
	    String text,
	    Map<String, Object> context
	) {}
