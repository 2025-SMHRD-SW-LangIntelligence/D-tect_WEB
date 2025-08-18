package com.smhrd.dtect.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import com.smhrd.dtect.service.AnalysisResultService;

@Controller
public class AnalysisResultController {
	
	@Autowired
	AnalysisResultService analysisResultService;
	
	public void reportWrite(@RequestParam Map<String, String> result, Model model) {
		
		Map<String, Object> jsondata = new HashMap<>();
		jsondata.put("anal_result", result.get("anal_result"));
		jsondata.put("anal_rate", result.get("anal_rate"));
		
		try {
			
			String url = "";
			
			WebClient webClient = WebClient.builder()
					.baseUrl(url)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.build();
			
			Map<String, Object> response = webClient.post()
					.bodyValue(jsondata)
					.retrieve()
					.bodyToMono(Map.class)
					.block();
			
			
			model.addAttribute("name", result.get("name"));
			model.addAttribute("result", response);
			
		} catch (Exception e) {
			
			model.addAttribute("error", "n8n측과 통신 실패: " + e.getMessage());
			
		}
		
	}
	
}
