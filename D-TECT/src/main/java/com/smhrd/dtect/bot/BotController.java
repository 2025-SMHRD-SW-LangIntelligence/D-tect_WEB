package com.smhrd.dtect.bot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.dtect.dto.bot.BotMessageRequest;
import com.smhrd.dtect.dto.bot.BotMessageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bot")
public class BotController {

    private final BotGateway botGateway;

    @PostMapping("/message")
    public ResponseEntity<BotMessageResponse> message(@RequestBody BotMessageRequest req) {
        BotMessageResponse res = botGateway.ask(req);
        return ResponseEntity.ok(res);
    }
}
