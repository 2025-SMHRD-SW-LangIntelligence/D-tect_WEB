package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smhrd.dtect.config.StorageProperties;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.AnalRate;
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
public class AnalysisReportCallbackController {

	private final AnalysisRepository analysisRepository;
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;
	private final AnalysisResultService analysisResultService;
	private final ReportStorageWriter writer; // NCP/Local 업로더
	private final StorageProperties storageProps;

	/*
	 * ========================= (A) JSON: URL로 전달 =========================
	 */
	@PostMapping(value = "/pdf-callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public ResponseEntity<PdfCallbackResponse> pdfReady(@RequestBody JsonNode root) {
	    JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

	    String sid        = text(node, "sid");
	    String username   = text(node, "username");
	    String analRateStr= firstNonBlank(text(node, "analRate"), text(node, "anal_rate"));
	    AnalRate rate     = AnalRateSafe.fromNullable(analRateStr);

	    JsonNode analResultNode = node.has("analResult") ? node.get("analResult")
	            : node.has("anal_result") ? node.get("anal_result") : null;
	    String analResultJson = (analResultNode == null) ? "" : analResultNode.toString();

	    String reportUrl = firstNonBlank(text(node, "reportUrl"), firstPdfUrl(node));
	    if (isBlank(reportUrl)) return badReq("missing reportUrl");

	    // ✅ sid로 analId 찾기 → 있으면 UPDATE, 없으면 기존 로직대로 생성
	    Long analId = analysisResultService.getAnalIdForSid(sid);

	    Analysis a;
	    if (analId != null) {
	        a = analysisRepository.findById(analId)
	                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));
	    } else {
	        // username 기반으로 생성 fallback
	        var member = memberRepository.findByUsername(username)
	                .orElseThrow(() -> new IllegalArgumentException("member not found: " + username));
	        var user = userRepository.findByMemberId(member.getMemIdx())
	                .orElseThrow(() -> new IllegalArgumentException("no tb_user row for member: " + username));

	        a = new Analysis();
	        a.setUser(user);
	        a.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
	    }

	    // 세션 시간
	    var started = analysisResultService.getStartedAt(sid);
	    var ended   = analysisResultService.getEndedAt(sid);

	    a.setAnalRate(rate != null ? rate : AnalRate.NORMAL);
	    a.setAnalResult(analResultJson);
	    a.setReportUrl(reportUrl);
	    if (started != null) a.setCreatedAt(java.sql.Timestamp.from(started));
	    if (ended   != null) a.setFinishedAt(java.sql.Timestamp.from(ended));

	    var saved = analysisRepository.save(a);
	    return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
	}

	/*
	 * ========================= (B) Multipart: 파일 직접 전달 =========================
	 */
	@PostMapping(value = "/pdf-callback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public ResponseEntity<PdfCallbackResponse> pdfReadyMultipart(@RequestParam(required = false) String sid,
			@RequestParam(required = false) String username,
			@RequestParam(required = false, name = "userId") String userIdStr, // 레거시 호환
			@RequestParam(required = false, defaultValue = "NORMAL") String analRate,
			@RequestParam(required = false) String analResult, @RequestPart("file") MultipartFile file)
			throws Exception {

		// ✅ username / userId(문자) / sid 로 사용자 해석
		User user = resolveUserCompat(username, userIdStr, sid);

		// 업로드
		String contentType = Optional.ofNullable(file.getContentType()).orElse("application/pdf");
		String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("report.pdf");
		String owner = !isBlank(sid) ? sid : (!isBlank(username) ? username : String.valueOf(user.getUserIdx()));

		String key = writer.uploadAutoName(owner, originalName, file.getBytes(), contentType);
		String reportUrl = buildPublicUrl(key);
		if (isBlank(reportUrl))
			return badReq("failed to build reportUrl from object key");

		// 세션 시각 반영
		Instant started = analysisResultService.getStartedAt(sid);
		Instant ended = analysisResultService.getEndedAt(sid);

		Analysis a = new Analysis();
		a.setUser(user);
		a.setAnalRate(AnalRateSafe.fromNullable(analRate));
		a.setAnalResult(Optional.ofNullable(analResult).orElse(""));
		a.setReportUrl(reportUrl);
		a.setCreatedAt(started != null ? Timestamp.from(started) : Timestamp.from(Instant.now()));
		a.setFinishedAt(ended != null ? Timestamp.from(ended) : null);

		Analysis saved = analysisRepository.save(a);
		return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
	}

	/*
	 * ========================= helpers =========================
	 */

	/** username → Member → User, fallback: sid → userId */
	private User resolveUser(String username, String sid) {
		String id = sanitize(username);

		if (!isBlank(id)) {
			// 1) 정확/느슨/유연 순으로 조회
			Member m = memberRepository.findByUsername(id).or(() -> memberRepository.findByUsernameIgnoreCaseTrim(id))
					.or(() -> memberRepository.findByAnyLoginId(id))
					.orElseThrow(() -> new IllegalArgumentException("member not found: " + id));

			// 2) Member → User 매핑 (없으면 명확 메시지)
			return userRepository.findByMemberId(m.getMemIdx()).orElseThrow(() -> new IllegalArgumentException(
					"no tb_user row for member username=" + m.getUsername() + " (mem_idx=" + m.getMemIdx() + ")"));
		}

		// username이 없으면 sid로 복원
		if (!isBlank(sid)) {
			Long uid = analysisResultService.getUserIdForSid(sid);
			if (uid != null) {
				return userRepository.findById(uid)
						.orElseThrow(() -> new IllegalArgumentException("user not found: " + uid));
			}
		}
		throw new IllegalArgumentException("username or sid is required");
	}

	/** 레거시(userIdStr) 포함 */
	private User resolveUserCompat(String username, String userIdStr, String sid) {
		if (!isBlank(username))
			return resolveUser(username, sid);

		if (!isBlank(userIdStr)) {
			try {
				Long uid = Long.valueOf(userIdStr);
				return userRepository.findById(uid)
						.orElseThrow(() -> new IllegalArgumentException("user not found: " + uid));
			} catch (NumberFormatException nfe) {
				// userId 필드에 username이 들어온 레거시 케이스
				return resolveUser(userIdStr, sid);
			}
		}
		return resolveUser(null, sid);
	}

	private static String sanitize(String s) {
		return s == null ? null : s.trim();
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

	// --- tiny utils ---
	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String trimRightSlash(String s) {
		return isBlank(s) ? null : s.replaceAll("/+$", "");
	}

	private static String text(JsonNode n, String key) {
		if (n == null || key == null)
			return null;
		JsonNode v = n.get(key);
		return (v == null || v.isNull()) ? null : (v.isTextual() ? v.asText() : v.toString());
	}

	private static String firstNonBlank(String... arr) {
		if (arr == null)
			return null;
		for (String s : arr)
			if (!isBlank(s))
				return s;
		return null;
	}

	/** results.pdf[0].url 추출 */
	private static String firstPdfUrl(JsonNode n) {
		if (n == null)
			return null;
		JsonNode pdf = n.get("pdf");
		if (pdf != null && pdf.isArray() && pdf.size() > 0) {
			JsonNode first = pdf.get(0);
			String url = first != null ? text(first, "url") : null;
			if (!isBlank(url))
				return url;
		}
		return null;
	}

	// 일관된 에러 응답
	private ResponseEntity<PdfCallbackResponse> badReq(String m) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PdfCallbackResponse(null, m));
	}

	private ResponseEntity<PdfCallbackResponse> notFound(String m) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new PdfCallbackResponse(null, m));
	}
}
