package com.smhrd.dtect.controller.payment;

import com.smhrd.dtect.dto.payment.PaymentReadyResponse;
import com.smhrd.dtect.dto.payment.TossConfirmRequest;
import com.smhrd.dtect.dto.payment.TossConfirmResponse;
import com.smhrd.dtect.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 사용자 결제 전: READY 생성
    @PostMapping("/invoices/{invoiceId}/ready")
    public ResponseEntity<PaymentReadyResponse> ready(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.ready(invoiceId));
    }

    // 토스 결제 승인(성공 리디렉션에서 호출)
    @PostMapping("/toss/confirm")
    public ResponseEntity<TossConfirmResponse> confirm(@RequestBody TossConfirmRequest req) {
        return ResponseEntity.ok(paymentService.confirm(req));
    }
}
