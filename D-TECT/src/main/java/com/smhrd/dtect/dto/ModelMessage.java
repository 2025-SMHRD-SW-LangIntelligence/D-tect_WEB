package com.smhrd.dtect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelMessage {

    private String user;
    private String text;

    // 점수는 문자열로 유지(상대편이 "0.92"로 내려주므로)
    private String score;

    // ★ 핵심: 항상 배열(List)로. 단일 객체로 와도 배열로 파싱하도록 허용
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<LabelCount> classification;
}