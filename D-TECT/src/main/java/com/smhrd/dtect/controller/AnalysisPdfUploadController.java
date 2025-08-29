package com.smhrd.dtect.controller;

import com.smhrd.dtect.config.StorageProperties;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.storage.ReportStorageWriter;
import com.smhrd.dtect.support.AnalRateSafe;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisPdfUploadController {

	private final AnalysisRepository analysisRepository;
	private final UserRepository userRepository;
	private final ReportStorageWriter writer;
	private final StorageProperties storageProps;
	private final AnalysisResultService analysisResultService;

	@PostMapping(value = "/pdf-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public ResponseEntity<PdfCallbackResponse> upload(@RequestParam(required = false) String sid,
			@RequestParam(required = false) Long userId, // user_idx
			@RequestParam(required = false, defaultValue = "NORMAL") String analRate,
			@RequestParam(required = false) String analResult, @RequestPart("file") MultipartFile file)
			throws Exception {

//		if (userId == null && sid != null)
//			userId = analysisResultService.getUserIdForSid(sid);
//		if (userId == null)
//			return unauthorized("userId missing");
//
//		User user = userRepository.findById(userId).orElse(null);
//		if (user == null)
//			return notFound("user not found: " + userId);

		String contentType = Optional.ofNullable(file.getContentType()).orElse("application/pdf");
		String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("report.pdf");

		String key = writer.uploadAutoName((sid == null || sid.isBlank()) ? String.valueOf(userId) : sid, originalName,
				file.getBytes(), contentType);
		String reportUrl = buildPublicUrl(key);
		if (isBlank(reportUrl)) {
			return ResponseEntity.badRequest().body(new PdfCallbackResponse(null, "failed to build reportUrl"));
		}

		Analysis a = new Analysis();
//        a.setUser(user);                                        // ★ 여기
        a.setAnalRate(AnalRateSafe.fromNullable(analRate));
        a.setAnalResult(Optional.ofNullable(analResult).orElse(""));
        a.setReportUrl(reportUrl);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
	}

	private String buildPublicUrl(String objectKey) {
		if (objectKey == null || objectKey.isBlank())
			return null;
		String provider = String.valueOf(storageProps.getProvider()).toLowerCase(Locale.ROOT);
		if ("ncp".equals(provider)) {
			String base = trimRightSlash(storageProps.getPublicBaseUrl());
			return (base != null) ? base + "/" + objectKey : null;
		}
		if ("local".equals(provider))
			return "/uploads/" + objectKey;
		return null;
	}

	private static String trimRightSlash(String s) {
		if (s == null || s.isBlank())
			return null;
		return s.replaceAll("/+$", "");
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
