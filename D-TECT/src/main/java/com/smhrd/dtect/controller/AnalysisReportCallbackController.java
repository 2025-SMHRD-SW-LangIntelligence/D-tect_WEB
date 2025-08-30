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

    /* =========================
       (A) JSON: URL만 전달
       ========================= */
    @PostMapping(
        value = "/pdf-callback",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfReady(@RequestBody JsonNode root) {
        JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

        String sid         = text(node, "sid");
        String usernameIn  = text(node, "username");
        String analRateStr = firstNonBlank(text(node, "analRate"), text(node, "anal_rate"));
        AnalRate rate      = AnalRateSafe.fromNullable(analRateStr);

        JsonNode analResultNode = node.has("analResult") ? node.get("analResult")
                : node.has("anal_result") ? node.get("anal_result") : null;
        String analResultJson = (analResultNode == null) ? "" : analResultNode.toString();

        String reportUrl = firstNonBlank(text(node, "reportUrl"), firstPdfUrl(node));
        if (isBlank(reportUrl)) return badReq("missing reportUrl (or results.pdf[0].url)");

        // sid → analId 있으면 기존 레코드 업데이트, 없으면 생성
        Long analId = analysisResultService.getAnalIdForSid(sid);
        Analysis a;
        if (analId != null) {
            a = analysisRepository.findById(analId)
                    .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));
        } else {
            // username이 없으면 sid에서 복원 시도
            String username = !isBlank(usernameIn) ? usernameIn : analysisResultService.getUsernameForSid(sid);
            if (isBlank(username)) return badReq("username missing and cannot be resolved from sid");

            User user = resolveUserByUsernameOnly(username); // MemberRepository의 현 메서드셋에 맞춘 해석
            a = new Analysis();
            a.setUser(user);
            // 세션 시작시각 있으면 주입, 없으면 now
            Instant started = analysisResultService.getStartedAt(sid);
            a.setCreatedAt(started != null ? Timestamp.from(started) : Timestamp.from(Instant.now()));
        }

        // 세션 시간 반영(기존 createdAt은 유지; 시작시각만 비어있을 때 세팅)
        Instant started = analysisResultService.getStartedAt(sid);
        Instant ended   = analysisResultService.getEndedAt(sid);

        a.setAnalRate(rate != null ? rate : AnalRate.NORMAL);
        a.setAnalResult(analResultJson);
        a.setReportUrl(reportUrl);
        if (a.getCreatedAt() == null && started != null) {
            a.setCreatedAt(Timestamp.from(started));
        }
        if (ended != null) a.setFinishedAt(Timestamp.from(ended));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
    }

    /* =========================
       (B) Multipart: 파일 직접 전달
       ========================= */
    @PostMapping(
        value = "/pdf-callback",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfReadyMultipart(
            @RequestParam(required = false) String sid,
            @RequestParam(required = false) String username,
            @RequestParam(required = false, name = "userId") String userIdStr, // 레거시 호환
            @RequestParam(required = false, defaultValue = "NORMAL") String analRate,
            @RequestParam(required = false) String analResult,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        // 업로드
        String contentType  = Optional.ofNullable(file.getContentType()).orElse("application/pdf");
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("report.pdf");

        // sid -> analId 가 있으면 기존 업데이트, 없으면 생성
        Long analId = analysisResultService.getAnalIdForSid(sid);
        Analysis a;

        if (analId != null) {
            a = analysisRepository.findById(analId)
                    .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));
        } else {
            // username → User (없으면 userIdStr 레거시 → 숫자 or username로 해석)
            User user = resolveUserCompat(username, userIdStr, sid);
            a = new Analysis();
            a.setUser(user);
            Instant started = analysisResultService.getStartedAt(sid);
            a.setCreatedAt(started != null ? Timestamp.from(started) : Timestamp.from(Instant.now()));
        }

        String owner = !isBlank(sid)
                ? sid
                : (!isBlank(username) ? username
                : String.valueOf(a.getUser().getUserIdx()));

        String key = writer.uploadAutoName(owner, originalName, file.getBytes(), contentType);
        String reportUrl = buildPublicUrl(key);
        if (isBlank(reportUrl)) return badReq("failed to build reportUrl from object key");

        a.setAnalRate(AnalRateSafe.fromNullable(analRate));
        a.setAnalResult(Optional.ofNullable(analResult).orElse(""));
        a.setReportUrl(reportUrl);

        Instant ended = analysisResultService.getEndedAt(sid);
        if (ended != null) a.setFinishedAt(Timestamp.from(ended));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
    }

    /* =========================
                 helpers
       ========================= */

    /** username 기준으로만 Member → User. MemberRepository 현 메서드셋에 맞춰 유연 처리 */
    private User resolveUserByUsernameOnly(String usernameRaw) {
        String username = sanitize(usernameRaw);
        if (isBlank(username)) throw new IllegalArgumentException("username is blank");

        // 1) username 매칭
        Optional<Member> mm = memberRepository.findByUsername(username);
        if (mm.isEmpty()) {
            // 2) email이면 이메일로
            if (username.contains("@")) {
                mm = memberRepository.findByEmail(username);
            } else {
                // 3) oauthProvider:oauthId 형태면 분리해서 조회
                int p = username.indexOf(':');
                if (p > 0) {
                    String provider = username.substring(0, p);
                    String oauthId  = username.substring(p + 1);
                    mm = memberRepository.findByOauthProviderAndOauthId(provider, oauthId);
                }
            }
        }
        Member m = mm.orElseThrow(() -> new IllegalArgumentException("member not found: " + username));

        return userRepository.findByMemberId(m.getMemIdx())
                .orElseThrow(() -> new IllegalArgumentException(
                        "no tb_user row for member username=" + m.getUsername() + " (mem_idx=" + m.getMemIdx() + ")"));
    }

    /** username 우선, 없으면 userIdStr(숫자 or username), 그래도 없으면 sid→username 복원 */
    private User resolveUserCompat(String username, String userIdStr, String sid) {
        if (!isBlank(username)) {
            return resolveUserByUsernameOnly(username);
        }

        if (!isBlank(userIdStr)) {
            try {
                Long uid = Long.valueOf(userIdStr);
                return userRepository.findById(uid)
                        .orElseThrow(() -> new IllegalArgumentException("user not found: " + uid));
            } catch (NumberFormatException nfe) {
                // userId 필드에 username이 들어온 레거시 케이스
                return resolveUserByUsernameOnly(userIdStr);
            }
        }

        // 마지막 폴백: sid → username 복원
        String uname = analysisResultService.getUsernameForSid(sid);
        if (!isBlank(uname)) {
            return resolveUserByUsernameOnly(uname);
        }

        throw new IllegalArgumentException("username or userId or sid is required");
    }

    private static String sanitize(String s) { return s == null ? null : s.trim(); }

    private String buildPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        String provider = String.valueOf(storageProps.getProvider()).toLowerCase(Locale.ROOT);
        if ("ncp".equals(provider)) {
            String base = trimRightSlash(storageProps.getPublicBaseUrl());
            return (base != null) ? base + "/" + objectKey : null;
        }
        if ("local".equals(provider)) return "/uploads/" + objectKey;
        return null;
    }

    // --- tiny utils ---
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trimRightSlash(String s) { return isBlank(s) ? null : s.replaceAll("/+$", ""); }
    private static String text(JsonNode n, String key) {
        if (n == null || key == null) return null;
        JsonNode v = n.get(key);
        return (v == null || v.isNull()) ? null : (v.isTextual() ? v.asText() : v.toString());
    }
    private static String firstNonBlank(String... arr) {
        if (arr == null) return null; for (String s : arr) if (!isBlank(s)) return s; return null;
    }
    /** results.pdf[0].url 추출 */
    private static String firstPdfUrl(JsonNode n) {
        if (n == null) return null;
        JsonNode pdf = n.get("pdf");
        if (pdf != null && pdf.isArray() && pdf.size() > 0) {
            JsonNode first = pdf.get(0);
            String url = first != null ? text(first, "url") : null;
            if (!isBlank(url)) return url;
        }
        return null;
    }

    private ResponseEntity<PdfCallbackResponse> badReq(String m){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PdfCallbackResponse(null, m));
    }
}
