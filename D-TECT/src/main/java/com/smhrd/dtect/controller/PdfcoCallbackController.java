package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.PdfcoResultItem;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.storage.ReportStorageClient;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class PdfcoCallbackController {

    /**
     * n8n에서 Body를 위 JSON 배열 그대로 보내면 됩니다.
     * 예: POST /api/analysis/pdfco-callback?sid=...&userId=123&analRate=WARNING
     */
	// 필요한 의존성 주입 (예: 생성자 주입)
	private final WebClient pdfWebClient;            // WebClient 빈 (그냥 builder로 만들어도 됨)
	private final AnalysisRepository analysisRepository;
	private final EntityManager em;                  // User 참조용
	private final ReportStorageClient storageClient; // NCP 업로드 클라이언트 (이미 사용 중인 것과 동일 인터페이스 가정)

	@PostMapping("/pdfco-callback")
	public ResponseEntity<?> handlePdfcoCallback(
	        @RequestParam("sid") String sid,
	        @RequestParam("userId") Long userId,
	        @RequestParam(value = "analRate", required = false) String analRate,
	        @RequestBody List<PdfcoResultItem> payload
	) {
	    if (payload == null || payload.isEmpty()) return ResponseEntity.badRequest().body("empty payload");
	    PdfcoResultItem item = payload.get(0);

	    // 1) PDF 다운로드
	    byte[] pdf = WebClient.create()
	            .get().uri(item.url())
	            .retrieve()
	            .bodyToMono(byte[].class)
	            .block();

	    // 2) 스토리지 업로드 (키는 예시)
	    String key = String.format("reports/%d/%s/%s", userId, sid, item.name());
	    //	storageClient.saveBytes(key, pdf);  // <-- 저장 메서드가 있다면 호출 (없으면 TODO)

	    // 3) Analysis 저장 (analResult/analRate/createdAt/reportPath 등 세팅)
	    //   - analResult에는 모델 원본 JSON을 넣는 게 일반적. 없다면 간단 요약/빈 문자열로.
	   
		    Analysis a = new Analysis();
		    a.setUser(em.getReference(User.class, userId));
		    a.setAnalResult("...모델 원본 JSON 또는 요약...");   // TODO: 적절히 채우기
		    a.setAnalRate(analRate != null ? AnalRate.valueOf(analRate) : AnalRate.NORMAL);
		    a.setReportPath(key);
		    a.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		    Analysis saved = analysisRepository.save(a);
	    

	    return ResponseEntity.ok(Map.of(
	            "sid", sid,
	            "userId", userId,
	            "fileStoredAs", key
	    ));
	}
}
