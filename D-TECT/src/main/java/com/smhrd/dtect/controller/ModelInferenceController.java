//package com.smhrd.dtect.controller;
//
//import com.smhrd.dtect.dto.LabelCount;
//import com.smhrd.dtect.dto.ModelMessage;
//import com.smhrd.dtect.dto.ModelResponse;
//import com.smhrd.dtect.service.ModelClient;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * 모델서버 연동 컨트롤러.
// * - 입력/출력 모두 classification은 배열(List<LabelCount>)
// */
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/model")
//@Slf4j
//public class ModelInferenceController {
//
//    private final ModelClient modelClient;
//
//    @PostMapping(value = "/infer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    public Mono<ModelResponse> infer(@RequestBody ModelMessage msg) {
//        String user = msg.getUser();
//        String text = msg.getText();
//        String score = msg.getScore();
//        List<LabelCount> classification = msg.getClassification() != null
//                ? msg.getClassification() : Collections.emptyList();
//
//        log.debug("[ModelInfer] user={}, textLen={}, classCount={}",
//                user, text != null ? text.length() : 0, classification.size());
//
//        return modelClient.infer(user, text, score, classification);
//    }
//
//    @GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ModelMessage ping() {
//        // ✅ 빌더 대신 생성자로 반환 (ModelMessage에 @Builder가 없다면 이게 안전)
//        return new ModelMessage("ping", "pong", "1.0", Collections.emptyList());
//    }
//}
