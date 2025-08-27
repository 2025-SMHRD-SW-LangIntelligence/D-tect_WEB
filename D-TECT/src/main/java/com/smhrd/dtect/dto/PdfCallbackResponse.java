package com.smhrd.dtect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfCallbackResponse {
    private Long analId;

    public static PdfCallbackResponse of(Long analId) {
        return PdfCallbackResponse.builder().analId(analId).build();
    }
}
