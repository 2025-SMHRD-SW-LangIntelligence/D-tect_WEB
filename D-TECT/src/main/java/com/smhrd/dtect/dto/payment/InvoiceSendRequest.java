package com.smhrd.dtect.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSendRequest {
    private String dueAt; // 발송 시점에 기한 업데이트/설정하고 싶을 때 사용
}
