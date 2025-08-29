package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.config.StorageProperties;
import com.smhrd.dtect.dto.PdfcoResultItem;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.storage.ReportStorageWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfIngestService {

    private final ReportStorageWriter writer;            // 객체 저장소에 업로드(키 반환)
    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;     // ✅ User → Member
    private final StorageProperties storageProps;        // publicBaseUrl 사용
    private final WebClient webClient = WebClient.builder().build();

    /* =========================
       권장: username 기반 API
       ========================= */

    /** PDFco(JSON) 응답의 url을 받아 다운로드 → 업로드 → Analysis 저장 */
    public Long saveFromJson(String username, String sid, AnalRate rate, String analResult,
                             List<PdfcoResultItem> list) throws Exception {
        if (isBlank(username)) throw new IllegalArgumentException("username is blank");
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty pdf list");

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + username));

        PdfcoResultItem first = list.get(0);
        byte[] bytes = download(Objects.requireNonNull(first.url(), "pdf[0].url is null"));
        String fileName = coalesce(first.name(), "report.pdf");
        String key = writer.uploadAutoName(coalesce(sid, username), fileName, bytes, MediaType.APPLICATION_PDF_VALUE);

        return persistAnalysis(member, rate, analResult, key);
    }

    /** 바이너리 직접 수신(멀티파트 등) → 업로드 → Analysis 저장 */
    public Long saveFromBytes(String username, String sid, AnalRate rate, String analResult,
                              String originalName, byte[] bytes) throws Exception {
        if (isBlank(username)) throw new IllegalArgumentException("username is blank");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("empty bytes");

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + username));

        String key = writer.uploadAutoName(coalesce(sid, username),
                coalesce(originalName, "report.pdf"),
                bytes,
                MediaType.APPLICATION_PDF_VALUE);

        return persistAnalysis(member, rate, analResult, key);
    }

    /* ==========================================
       호환용(기존 Long ID 호출부가 남아있다면)
       ========================================== */

    /** @deprecated 기존 Long 기반 호출부 호환용 (가능하면 username 버전으로 교체) */
    @Deprecated
    public Long saveFromJson(Long memberId, String sid, AnalRate rate, String analResult,
                             List<PdfcoResultItem> list) throws Exception {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + memberId));
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty pdf list");
        PdfcoResultItem first = list.get(0);
        byte[] bytes = download(Objects.requireNonNull(first.url(), "pdf[0].url is null"));
        String fileName = coalesce(first.name(), "report.pdf");
        String key = writer.uploadAutoName(coalesce(sid, String.valueOf(memberId)), fileName, bytes, MediaType.APPLICATION_PDF_VALUE);
        return persistAnalysis(member, rate, analResult, key);
    }

    /** @deprecated 기존 Long 기반 호출부 호환용 (가능하면 username 버전으로 교체) */
    @Deprecated
    public Long saveFromBytes(Long memberId, String sid, AnalRate rate, String analResult,
                              String originalName, byte[] bytes) throws Exception {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + memberId));
        String key = writer.uploadAutoName(coalesce(sid, String.valueOf(memberId)),
                coalesce(originalName, "report.pdf"),
                bytes,
                MediaType.APPLICATION_PDF_VALUE);
        return persistAnalysis(member, rate, analResult, key);
    }

    /* =========================
              내부 유틸
       ========================= */

    private byte[] download(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("download failed: " + url));
    }

    /** Analysis(회원 소유, URL만 저장) 영속화 */
    private Long persistAnalysis(Member member, AnalRate rate, String analResult, String objectKey) {
        String reportUrl = buildPublicUrl(objectKey);  // ✅ DB엔 URL만
        if (isBlank(reportUrl)) {
            throw new IllegalStateException("failed to build public url from key: " + objectKey);
        }

        Analysis a = new Analysis();
//        a.setMember(member);
        a.setAnalRate(rate != null ? rate : AnalRate.NORMAL);
        a.setAnalResult(coalesce(analResult, "자동 생성된 분석 보고서"));
        a.setReportUrl(reportUrl);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        log.info("[PDF] saved analysis analId={} username={} url={}",
                saved.getAnalIdx(), member.getUsername(), reportUrl);
        return saved.getAnalIdx();
    }

    private String buildPublicUrl(String objectKey) {
        if (isBlank(objectKey)) return null;
        String provider = String.valueOf(storageProps.getProvider()).toLowerCase(Locale.ROOT);

        if ("ncp".equals(provider)) {
            String base = trimRightSlash(storageProps.getPublicBaseUrl());
            return base != null ? base + "/" + objectKey : null;
        }
        if ("local".equals(provider)) {
            return "/uploads/" + objectKey; // WebConfig로 정적 서빙
        }
        return null;
    }

    private static String trimRightSlash(String s) {
        if (isBlank(s)) return null;
        return s.replaceAll("/+$", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String coalesce(String a, String b) {
        return isBlank(a) ? b : a;
    }
}
