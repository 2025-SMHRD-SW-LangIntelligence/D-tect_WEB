package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.smhrd.dtect.service.pdf.PdfLayout.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfOverlayReportService {

    private final AnalysisRepository analysisRepository;
    private final CaseRepository caseRepository;

    private static final String TEMPLATE_PDF = "pdf/dtect-report-template.pdf";
    private static final String FONT_KR      = "fonts/NotoSansKR-Regular.otf";

    private static final Map<FieldName, String> LABEL_KR = Map.of(
            FieldName.VIOLENCE,   "폭력",
            FieldName.DEFAMATION, "명예훼손",
            FieldName.SEXUAL,     "성범죄",
            FieldName.BULLYING,   "따돌림/집단괴롭힘",
            FieldName.CHANTAGE,   "협박/갈취",
            FieldName.EXTORTION,  "공갈/강요"
    );

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    @Transactional(readOnly = true)
    public byte[] renderPdfBytes(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        // 메타
        Timestamp created  = a.getCreatedAt();
        Timestamp finished = (a.getFinishedAt() != null ? a.getFinishedAt() : a.getCreatedAt());
        String createdAt   = created  != null ? DF.format(created)  : "-";
        String period      = (created != null && finished != null)
                ? DF.format(created) + " ~ " + DF.format(finished) : "-";
        String rateKr      = (a.getAnalRate() != null ? toKrRate(a.getAnalRate()) : "-");

        // 집계
        EnumMap<FieldName, Long> counts = new EnumMap<>(FieldName.class);
        List<CaseRepository.TypeCount> list = caseRepository.countByAnalysisIdGroupByType(analId);
        for (CaseRepository.TypeCount tc : list) {
            if (tc.getCnt() > 0) counts.put(tc.getType(), tc.getCnt());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        try (PDDocument doc = PDDocument.load(new ClassPathResource(TEMPLATE_PDF).getInputStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDType0Font font = PDType0Font.load(doc, new ClassPathResource(FONT_KR).getInputStream(), true);
            PDPage page = doc.getPage(0);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // 메타 텍스트
                drawText(cs, font, FONT_SIZE_BODY, META_CREATED_AT_X, META_CREATED_AT_Y, "생성일: " + createdAt);
                drawText(cs, font, FONT_SIZE_BODY, META_PERIOD_X,     META_PERIOD_Y,     "분석 기간: " + period);
                drawText(cs, font, FONT_SIZE_BODY, META_RATE_X,       META_RATE_Y,       "분석 등급: " + rateKr);

                // 유형별 “N회” (0회는 미표시)
                for (var e : TABLE_ROW_Y.entrySet()) {
                    FieldName fn = e.getKey();
                    long cnt = counts.getOrDefault(fn, 0L);
                    if (cnt <= 0) continue; // 0회는 출력 안 함
                    float y = e.getValue();
                    drawText(cs, font, FONT_SIZE_BODY, TABLE_COUNT_X, y, cnt + "회");
                }

                // 총계
                drawText(cs, font, FONT_SIZE_BODY, SUMMARY_TOTAL_X, SUMMARY_TOTAL_Y, "총계: " + total + "회");
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF render failed: " + e.getMessage(), e);
        }
    }

    private static void drawText(PDPageContentStream cs, PDType0Font font, float size, float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private static String toKrRate(AnalRate rate) {
        return switch (rate) {
            case DANGER  -> "위험";
            case WARNING -> "주의";
            default      -> "정상";
        };
    }
}
