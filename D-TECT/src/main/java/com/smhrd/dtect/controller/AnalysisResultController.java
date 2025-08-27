package com.smhrd.dtect.controller;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Controller
public class AnalysisResultController {

    @PostMapping("/analysis")
    public String finalizeBySid(@RequestParam("sid") String sid,
                                @RequestParam(value = "username", required = false) String username,
                                Model model) {
        try {
            String url = "http://192.168.219.45:8081/api/analysis/finalize?sid=" + sid;

            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<?,?> response = webClient.post()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            model.addAttribute("sid", sid);
            model.addAttribute("username", username);
            model.addAttribute("result", response);
            return "analysis/result"; // 필요시 뷰 이름
        } catch (Exception e) {
            model.addAttribute("error", "finalize 호출 실패: " + e.getMessage());
            return "analysis/error";
        }
    }
}
