package com.smhrd.dtect.controller.payment;

import com.smhrd.dtect.dto.payment.TossConfirmRequest;
import com.smhrd.dtect.dto.payment.TossConfirmResponse;
import com.smhrd.dtect.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PaymentReturnController {

    private final PaymentService paymentService;

    // Toss successUrl 으로 설정한 엔드포인트 (GET)
    @GetMapping("/pay/success")
    public String paySuccess(
            @RequestParam String orderId,
            @RequestParam String paymentKey,
            @RequestParam Integer amount,
            Model model
    ) {
        // 서버에서 최종 승인
        TossConfirmResponse res = paymentService.confirm(
                TossConfirmRequest.builder()
                        .orderId(orderId)
                        .paymentKey(paymentKey)
                        .amount(amount)
                        .build()
        );
        model.addAttribute("res", res);
        model.addAttribute("amount", amount);
        return "pay/success"; // templates/payment/success.html
    }

    // Toss fail
    @GetMapping("/pay/fail")
    public String payFail(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message,
            Model model
    ) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "pay/fail";
    }
}
