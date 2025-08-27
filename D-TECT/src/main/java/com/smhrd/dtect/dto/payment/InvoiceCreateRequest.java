package com.smhrd.dtect.dto.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCreateRequest {
    private Long matchingId;      // 필수
    private String title;         // 옵션(없으면 "상담료 청구서")
    private String description;   // 내용

    @JsonAlias("amount")
    private Integer amountTotal;  // 결제 금액
    private String dueAt;         // 시각
}