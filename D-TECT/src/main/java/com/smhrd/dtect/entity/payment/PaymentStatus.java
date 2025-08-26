package com.smhrd.dtect.entity.payment;

public enum PaymentStatus {
    READY,        // 결제 시도 생성
    DONE,         // 승인 완료
    FAILED,       // 실패
    CANCELED      // 사용자/시스템 취소
}