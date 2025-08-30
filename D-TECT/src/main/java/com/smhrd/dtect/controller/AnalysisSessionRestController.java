package com.smhrd.dtect.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smhrd.dtect.service.AnalysisResultService;

@RestController
@RequestMapping("/api/analysis/session")
@RequiredArgsConstructor
public class AnalysisSessionRestController {
	
	private final AnalysisResultService analysisResultService;
	
	

    // 종료 트리거(옵션): 캡처 종료 시 호출하면 n8n 디스패치 즉시 수행
    @PostMapping("/end")
    public Map<String,Object> end(@RequestParam String sid) {
        analysisResultService.markEnded(sid);
        boolean ok = analysisResultService.finalizeNow(sid); // n8n으로 counts 전송
        return Map.of("ok", ok);
    }

    // 폴링: SID → analId (없으면 null)
    @GetMapping("/anal-id")
    public Map<String,Object> getAnalId(@RequestParam String sid) {
        Long analId = analysisResultService.getAnalIdForSid(sid);
        return Map.of("analId", analId);
    }
}