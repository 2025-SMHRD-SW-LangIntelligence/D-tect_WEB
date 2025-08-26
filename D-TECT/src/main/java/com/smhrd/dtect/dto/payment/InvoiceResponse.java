package com.smhrd.dtect.dto.payment;

import com.smhrd.dtect.entity.payment.InvoiceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private Long matchingId;
    private Long expertId;
    private Long userId;

    private String title;
    private String description;
    private Integer amountTotal;
    private String currency; // KRW

    private InvoiceStatus status;

    private String issuedAt;
    private String dueAt;
    private String paidAt;
    private String canceledAt;

    private String createdAt;
    private String updatedAt;
}