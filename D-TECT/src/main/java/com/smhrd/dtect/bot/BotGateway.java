package com.smhrd.dtect.bot;

import com.smhrd.dtect.dto.BotMessageRequest;
import com.smhrd.dtect.dto.BotMessageResponse;

public interface BotGateway {
    BotMessageResponse ask(BotMessageRequest request);
}
