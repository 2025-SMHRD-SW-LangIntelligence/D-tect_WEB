package com.smhrd.dtect.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossConfirmRequest {
    private String paymentKey;
    private String orderId;
    private Integer amount;
}
