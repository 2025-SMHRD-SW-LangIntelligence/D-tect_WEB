package com.smhrd.dtect.bot;

import com.smhrd.dtect.dto.bot.BotMessageRequest;
import com.smhrd.dtect.dto.bot.BotMessageResponse;

public interface BotGateway {
    BotMessageResponse ask(BotMessageRequest request);
}
