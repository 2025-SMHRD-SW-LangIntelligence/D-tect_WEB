package com.smhrd.dtect.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReadyResponse {
    private Long invoiceId;
    private String orderId;
    private Integer amount;   // KRW
    private String status;    // READY
}