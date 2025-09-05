package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.entity.FieldName;
import java.util.EnumMap;
import java.util.Map;

// PDF 기본 Layout
public final class PdfLayout {
    private PdfLayout(){}

    public static final float FONT_SIZE_TITLE = 16f;
    public static final float FONT_SIZE_BODY  = 11f;

    // 메타 영역
    public static final float META_CREATED_AT_X = 60f;
    public static final float META_CREATED_AT_Y = 780f;
    public static final float META_PERIOD_X     = 60f;
    public static final float META_PERIOD_Y     = 760f;
    public static final float META_RATE_X       = 60f;
    public static final float META_RATE_Y       = 740f;

    // 표: 오른쪽 열(“N회”)에 찍는 X좌표
    public static final float TABLE_COUNT_X     = 480f;

    // 각 라인의 Y좌표
    public static final Map<FieldName, Float> TABLE_ROW_Y;
    static {
        EnumMap<FieldName, Float> m = new EnumMap<>(FieldName.class);
        m.put(FieldName.VIOLENCE,   680f);
        m.put(FieldName.DEFAMATION, 660f);
        m.put(FieldName.SEXUAL,     640f);
        m.put(FieldName.BULLYING,   620f);
        m.put(FieldName.CHANTAGE,   600f);
        m.put(FieldName.EXTORTION,  580f);
        TABLE_ROW_Y = Map.copyOf(m);
    }

    // 총계
    public static final float SUMMARY_TOTAL_X   = 60f;
    public static final float SUMMARY_TOTAL_Y   = 540f;
}
