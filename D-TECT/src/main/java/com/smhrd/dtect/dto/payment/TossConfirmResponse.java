package com.smhrd.dtect.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossConfirmResponse {
    private Long paymentId;
    private Long invoiceId;
    private String orderId;
    private String paymentKey;
    private String status;
    private String approvedAt;
    private String method;
    private String receiptUrl;   // 있을 때만
}