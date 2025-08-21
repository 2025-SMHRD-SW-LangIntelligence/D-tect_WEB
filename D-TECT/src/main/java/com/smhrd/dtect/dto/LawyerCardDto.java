package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.Field;
import com.smhrd.dtect.entity.FieldName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LawyerCardDto {
    private Long id;                 // expertIdx
    private String name;             // 이름
    private String title;            // "법률 전문가"
    private Integer years;           // 확장성 고려
    private String phone;
    private String email;
    private String addr;             // expert.officeAddress
    private List<String> skills;     // 한글 라벨
    private List<String> skillCodes;
    private boolean available;       // 승인된 전문가 → true

    public static LawyerCardDto from(Expert e, List<Field> fields) {
        var m = e.getMember();

        List<String> codes  = fields.stream().map(f -> f.getFieldName().name()).toList();
        List<String> labels = fields.stream().map(f -> toKo(f.getFieldName())).toList();

        String addr = (e.getOfficeAddress() != null && !e.getOfficeAddress().isBlank())
                ? e.getOfficeAddress() : m.getAddress();

        return new LawyerCardDto(
                e.getExpertIdx(),
                m.getName(),
                "법률 전문가",
                null,               // years 확장용
                m.getPhone(),
                m.getEmail(),
                addr,
                labels,
                codes,
                e.getExpertStatus() == ExpertStatus.APPROVED
        );
    }

    private static String toKo(FieldName n) {
        return switch (n) {
            case VIOLENCE  -> "폭력";
            case DEFAMATION-> "명예훼손";
            case STALKING  -> "스토킹";
            case SEXUAL    -> "성희롱·성폭력";
            case LEAK      -> "정보유출";
            case BULLYING  -> "따돌림·괴롭힘";
            case CHANTAGE  -> "협박";
            case EXTORTION -> "갈취";
        };
    }
}
