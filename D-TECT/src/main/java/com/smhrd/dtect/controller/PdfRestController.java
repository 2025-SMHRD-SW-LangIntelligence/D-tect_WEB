package com.smhrd.dtect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Upload;

@RestController
public class PdfRestController {
	

	    @PostMapping("/send-to-n8n")
	    public ResponseEntity<?> sendToN8n(@RequestBody Upload result) {
	        RestTemplate restTemplate = new RestTemplate();

	        String n8nUrl = "http://n8nKHS.n8nKHS.com/webhook/7f2cb27a-316c-4675-aa52-f400bac7a117"; // n8n Webhook URL
	        ResponseEntity<String> response = restTemplate.postForEntity(n8nUrl, result, String.class);

	        return ResponseEntity.ok(response.getBody());
	    }
	    

	        @PostMapping("/pdf-result")
	        public ResponseEntity<?> receivePdfResult(@RequestBody Analysis pdfResult) {
	            // 1. NCP Object Storage 업로드 (pdfResult.getFileUrl()에서 다운로드 후 업로드)
	            // 2. 업로드 경로 DB 저장

	            return ResponseEntity.ok("PDF 저장 프로세스 완료");
	        }


	
	
}
