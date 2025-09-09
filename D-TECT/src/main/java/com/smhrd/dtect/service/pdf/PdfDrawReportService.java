package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.entity.analysis.AnalRate;
import com.smhrd.dtect.entity.field.FieldName;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfDrawReportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final String FONT_PATH = "fonts/NotoSansKR-Regular.ttf";

    public byte[] render(
            Long analId,
            String displayName,
            EnumMap<FieldName, Integer> counts,
            AnalRate rate,
            Timestamp createdAt,
            Timestamp finishedAt
    ) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType0Font font = PDType0Font.load(doc, new ClassPathResource(FONT_PATH).getInputStream(), true);

            PDRectangle box = page.getMediaBox();
            float margin = 56f;
            float x = margin;
            float y = box.getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                writeText(cs, font, 20, x, y, "사이버불링 탐지 결과 보고서");
                y -= 14;
                drawLine(cs, x, y, box.getWidth() - margin, y);
                y -= 18;

                // 양식
                String meta1 = "사용자: " + (displayName == null || displayName.isBlank() ? "사용자" : displayName);
                String meta2 = "분석 ID: #" + analId;
                String meta3 = "분석 기간: " +
                        (createdAt != null ? DATE_FMT.format(createdAt.toInstant()) : "-") +
                        " ~ " +
                        (finishedAt != null ? DATE_FMT.format(finishedAt.toInstant()) : "-");
                String meta4 = "분석 등급: " + (rate != null ? rate.name() : "NORMAL");

                writeText(cs, font, 11, x, y, meta1); y -= 16;
                writeText(cs, font, 11, x, y, meta2); y -= 16;
                writeText(cs, font, 11, x, y, meta3); y -= 16;
                writeText(cs, font, 11, x, y, meta4); y -= 20;

                writeText(cs, font, 14, x, y, "유형별 검출 내역"); y -= 12;
                drawLine(cs, x, y, box.getWidth() - margin, y); y -= 14;

                float tableX = x;
                float col1 = 240f; // 유형
                float col2 = 100f; // 횟수
                float rowH = 18f;

                String[][] rows = new String[][]{
                        {"폭력", countStr(counts, FieldName.VIOLENCE)},
                        {"명예훼손", countStr(counts, FieldName.DEFAMATION)},
                        {"성범죄", countStr(counts, FieldName.SEXUAL)},
                        {"따돌림/집단괴롭힘", countStr(counts, FieldName.BULLYING)},
                        {"협박/갈취", countStr(counts, FieldName.CHANTAGE)},
                        {"공갈/강요", countStr(counts, FieldName.EXTORTION)}
                };

                // 0회는 출력하지 않음
                for (String[] r : rows) {
                    if (r[1].equals("0")) continue;
                    drawLine(cs, tableX, y, tableX + col1 + col2, y);
                    y -= rowH - 4;
                    writeText(cs, font, 11, tableX + 6, y, r[0]);
                    writeTextRight(cs, font, 11, tableX + col1 + col2 - 6, y, r[1] + "회");
                    y -= 4;
                }

                drawLine(cs, tableX, y, tableX + col1 + col2, y);
                y -= 18;

                // 요약
                writeText(cs, font, 14, x, y, "요약"); y -= 12;
                drawLine(cs, x, y, box.getWidth() - margin, y); y -= 14;

                // 간단 요약 문장
                String summary = buildSummary(counts, rate);
                writeParagraph(cs, font, 11, x, y, summary, box.getWidth() - margin - x, 16);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                doc.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패: " + e.getMessage(), e);
        }
    }

    private static void writeText(PDPageContentStream cs, PDType0Font font, int size, float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private static void writeTextRight(PDPageContentStream cs, PDType0Font font, int size, float rightX, float y, String text) throws Exception {
        float w = font.getStringWidth(text) / 1000 * size;
        writeText(cs, font, size, rightX - w, y, text);
    }

    private static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws Exception {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static String countStr(Map<FieldName,Integer> m, FieldName f) {
        return String.valueOf(Math.max(0, m.getOrDefault(f, 0)));
    }

    private static String buildSummary(Map<FieldName,Integer> counts, AnalRate rate) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("본 분석에서는 총 ").append(total).append("건의 사이버불링 관련 징후가 탐지되었습니다. ");

        if (rate == AnalRate.DANGER) {
            sb.append("위험 징후가 높게 나타났으므로 즉각적인 보호 조치를 권고합니다. ");
        } else if (rate == AnalRate.WARNING) {
            sb.append("주의 단계로 판단되며, 지속 모니터링과지도·상담을 권고합니다. ");
        } else {
            sb.append("현재는 정상 범주로 평가됩니다. ");
        }

        counts.entrySet().stream()
                .sorted((a,b)-> Integer.compare(b.getValue(), a.getValue()))
                .limit(2)
                .filter(e -> e.getValue() > 0)
                .forEach(e -> {
                    sb.append(typeKo(e.getKey()))
                            .append(" ")
                            .append(e.getValue()).append("회");
                    sb.append(" ");
                });

        return sb.toString().getBytes(StandardCharsets.UTF_8).length > 0 ? sb.toString() : "요약 정보가 없습니다.";
    }

    private static String typeKo(FieldName f) {
        return switch (f) {
            case VIOLENCE -> "폭력";
            case DEFAMATION -> "명예훼손";
            case SEXUAL -> "성범죄";
            case BULLYING -> "따돌림/집단괴롭힘";
            case CHANTAGE -> "협박/갈취";
            case EXTORTION -> "공갈/강요";
        };
    }

    private static void writeParagraph(PDPageContentStream cs, PDType0Font font, int size,
                                       float x, float y, String text, float maxWidth, float lineHeight) throws Exception {
        String[] words = (text == null ? "" : text).split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String test = (line.length() == 0 ? w : line + " " + w);
            float width = font.getStringWidth(test) / 1000 * size;
            if (width > maxWidth) {
                writeText(cs, font, size, x, y, line.toString());
                y -= lineHeight;
                line.setLength(0);
                line.append(w);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(w);
            }
        }
        if (line.length() > 0) writeText(cs, font, size, x, y, line.toString());
    }
}
