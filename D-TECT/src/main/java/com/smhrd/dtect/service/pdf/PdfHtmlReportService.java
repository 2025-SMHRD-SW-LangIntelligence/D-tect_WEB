package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfHtmlReportService {

    private static final String TEMPLATE_PATH = "pdf/dtect-report-template.html";
    private static final String FONT_PATH     = "fonts/NotoSansKR-Regular.ttf";

    private static final Map<FieldName, String> LABEL_KR = Map.of(
            FieldName.VIOLENCE,   "폭력",
            FieldName.DEFAMATION, "명예훼손",
            FieldName.SEXUAL,     "성범죄",
            FieldName.BULLYING,   "따돌림/집단괴롭힘",
            FieldName.CHANTAGE,   "협박/갈취",
            FieldName.EXTORTION,  "공갈/강요"
    );
    private static final Map<AnalRate, String> RATE_KO = Map.of(
            AnalRate.NORMAL,  "정상",
            AnalRate.WARNING, "주의",
            AnalRate.DANGER,  "위험"
    );

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    private static final DecimalFormat    NF = new DecimalFormat("#,###");

    public byte[] render(
            Long analId,
            String sid,
            String username,
            String displayName,        // 사용자 이름
            Map<FieldName, Integer> counts,
            AnalRate rate,
            Instant startedAt,
            Instant endedAt
    ) {
        try {
            String html = loadTemplate();

            final String today = DF.format(new Date());
            final String started = (startedAt != null) ? DF.format(Date.from(startedAt)) : "-";
            final String ended   = (endedAt   != null) ? DF.format(Date.from(endedAt))   : "-";
            final String period  = started + " ~ " + ended;

            int total = counts == null ? 0 : counts.values().stream().mapToInt(Integer::intValue).sum();

            String rowsHtml = buildRows(counts);

            String usernameLine = (username != null && !username.isBlank())
                    ? "<div><b>사용자 ID</b> : " + esc(username) + "</div>"
                    : "";

            html = html.replace("{{TITLE_NAME}}", esc(defaultIfBlank(displayName, "사용자")))
                    .replace("{{ANAL_ID}}", analId != null ? String.valueOf(analId) : "-")
                    .replace("{{SID}}", sid != null ? esc(sid) : "-")
                    .replace("{{TODAY}}", esc(today))
                    .replace("{{PERIOD}}", esc(period))
                    .replace("{{ANAL_RATE}}", rate != null ? rate.name() : "NORMAL")
                    .replace("{{ANAL_RATE_KO}}", RATE_KO.getOrDefault(rate, "정상"))
                    .replace("{{USERNAME_LINE}}", usernameLine)
                    .replace("{{ROWS}}", rowsHtml)
                    .replace("{{TOTAL}}", NF.format(total));

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                // 한글 폰트 등록 (CSS에서 'Noto Sans KR'로 사용)
                builder.useFont(
                        () -> {
                            try {
                                return new ClassPathResource(FONT_PATH).getInputStream();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        "Noto Sans KR"
                );
                builder.toStream(out);
                builder.run();
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("HTML->PDF 렌더 실패: {}", e.toString(), e);
            throw new RuntimeException("HTML PDF 생성 실패: " + e.getMessage(), e);
        }
    }

    private static String buildRows(Map<FieldName, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<tr><td colspan=\"2\" class=\"empty\">탐지된 항목이 없습니다.</td></tr>";
        }

        List<FieldName> order = Arrays.asList(
                FieldName.VIOLENCE, FieldName.DEFAMATION, FieldName.SEXUAL,
                FieldName.BULLYING, FieldName.CHANTAGE, FieldName.EXTORTION
        );
        StringBuilder sb = new StringBuilder();
        for (FieldName f : order) {
            int v = Math.max(0, counts.getOrDefault(f, 0));
            if (v <= 0) continue; // 0회는 출력 안 함
            sb.append("<tr>")
                    .append("<td>").append(esc(LABEL_KR.getOrDefault(f, f.name()))).append("</td>")
                    .append("<td class=\"count\">").append(NF.format(v)).append("</td>")
                    .append("</tr>");
        }
        if (sb.length() == 0) {
            return "<tr><td colspan=\"2\" class=\"empty\">탐지된 항목이 없습니다.</td></tr>";
        }
        return sb.toString();
    }

    private static String loadTemplate() throws Exception {
        try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"","&quot;")
                .replace("'", "&#39;");
    }
    private static String defaultIfBlank(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
