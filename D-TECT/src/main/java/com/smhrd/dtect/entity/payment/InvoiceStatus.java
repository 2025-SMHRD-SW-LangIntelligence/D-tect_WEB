package com.smhrd.dtect.entity.payment;

public enum InvoiceStatus {
    DRAFT,     // 작성 중 (전문가만 보임)
    SENT,      // 사용자에게 노출/청구
    PAID,      // 결제 완료
    CANCELED
}