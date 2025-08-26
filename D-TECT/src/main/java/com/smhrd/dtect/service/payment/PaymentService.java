package com.smhrd.dtect.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.payment.PaymentReadyResponse;
import com.smhrd.dtect.dto.payment.TossConfirmRequest;
import com.smhrd.dtect.dto.payment.TossConfirmResponse;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import com.smhrd.dtect.entity.payment.*;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.payment.InvoiceRepository;
import com.smhrd.dtect.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MatchingRepository matchingRepository;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 토스 테스트 시크릿키
    private static final String TOSS_SECRET_KEY = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";

    private static String randomSuffix(int n) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
    private static Timestamp nowTs() { return new Timestamp(System.currentTimeMillis()); }
    private Map<String, Object> readJson(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (Exception e) { throw new RuntimeException("토스 응답 파싱 실패", e); }
    }
    private static String str(Object o) { return (o == null) ? null : String.valueOf(o); }
    private static Timestamp parseTsSafe(String iso) {
        if (iso == null || iso.isBlank()) return nowTs();
        try { return Timestamp.from(Instant.parse(iso)); }
        catch (DateTimeParseException e) { return nowTs(); }
    }

    // 결제 READY
    // 사용자 결제창 띄우기 전에 결제시도(READY)
    @Transactional
    public PaymentReadyResponse ready(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("청구서가 없습니다. id=" + invoiceId));

        if (inv.getStatus() != InvoiceStatus.SENT) {
            throw new IllegalStateException("SENT 상태의 청구서만 결제할 수 있습니다.");
        }

        Integer amount = inv.getAmountTotal();
        if (amount == null || amount <= 0) {
            throw new IllegalStateException("유효하지 않은 청구 금액입니다.");
        }

        // orderId: {invoiceId}_{random}
        String orderId = invoiceId + "_" + randomSuffix(8);

        Payment p = new Payment();
        p.setInvoice(inv);
        p.setProvider(PaymentProvider.TOSS);
        p.setOrderId(orderId);
        p.setAmount(amount);
        p.setStatus(PaymentStatus.READY);
        p.setRequestedAt(nowTs());

        Payment saved = paymentRepository.save(p);

        return PaymentReadyResponse.builder()
                .invoiceId(inv.getInvoiceId())
                .orderId(saved.getOrderId())
                .amount(saved.getAmount())
                .status(saved.getStatus().name())
                .build();
    }

    // 토스 승인
    // 토스 결제 최종 승인
    @Transactional
    public TossConfirmResponse confirm(TossConfirmRequest req) {
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 승인 금액입니다.");
        }
        if (req.getOrderId() == null || req.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId 없음");
        }
        if (req.getPaymentKey() == null || req.getPaymentKey().isBlank()) {
            throw new IllegalArgumentException("paymentKey 없음");
        }

        Payment p = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 시도를 찾을 수 없습니다. orderId=" + req.getOrderId()));

        if (p.getStatus() != PaymentStatus.READY) {
            throw new IllegalStateException("이미 처리된 결제거나 상태가 유효하지 않습니다. status=" + p.getStatus());
        }

        Invoice inv = p.getInvoice();
        if (inv.getStatus() != InvoiceStatus.SENT) {
            throw new IllegalStateException("청구서 상태가 SENT가 아닙니다.");
        }

        // 금액 검증
        if (!req.getAmount().equals(p.getAmount()) || !req.getAmount().equals(inv.getAmountTotal())) {
            throw new IllegalStateException("승인요청 금액과 청구 금액이 일치하지 않습니다.");
        }

        // Toss 승인 API 호출
        String base64Secret = Base64.getEncoder()
                .encodeToString((TOSS_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + base64Secret);
        headers.set("Accept-Language", "en-US");

        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );

        ResponseEntity<String> res = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/confirm",
                new HttpEntity<>(body, headers),
                String.class
        );
        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("토스 승인 실패: " + res.getBody());
        }

        Map<String, Object> toss = readJson(res.getBody());

        String status = str(toss.get("status"));            // DONE
        String approvedAtStr = str(toss.get("approvedAt"));
        String method = str(toss.get("method"));            // CARD 등

        // 영수증 URL (receiptUrl 또는 receipt.url)
        String receiptUrl = str(toss.get("receiptUrl"));
        if (receiptUrl == null && toss.get("receipt") instanceof Map) {
            receiptUrl = str(((Map<?, ?>) toss.get("receipt")).get("url"));
        }

        // 카드 정보
        if (toss.get("card") instanceof Map card) {
            p.setCardCompany(str(card.get("issuerCode")));
            p.setCardBin(str(card.get("bin")));
            String num = str(card.get("number"));
            if (num != null && num.length() >= 4) {
                p.setCardLast4(num.substring(num.length() - 4));
            }
        }

        // DB 반영
        p.setPaymentKey(req.getPaymentKey());
        p.setMethod(method);
        p.setReceiptUrl(receiptUrl);
        p.setStatus("DONE".equalsIgnoreCase(status) ? PaymentStatus.DONE : PaymentStatus.FAILED);
        p.setApprovedAt(parseTsSafe(approvedAtStr));
        p.setRawPayloadJson(res.getBody());
        paymentRepository.save(p);

        if (p.getStatus() == PaymentStatus.DONE) {
            inv.setStatus(InvoiceStatus.PAID);
            inv.setPaidAt(p.getApprovedAt() != null ? p.getApprovedAt() : nowTs());
            invoiceRepository.save(inv);

            // 매칭 상태 = PAID 로 업데이트
            Matching m = inv.getMatching();
            if (m != null && m.getStatus() != MatchingStatus.PAID) {
                m.setStatus(MatchingStatus.PAID);
                matchingRepository.save(m);
            }
        }

        return TossConfirmResponse.builder()
                .paymentId(p.getPaymentId())
                .invoiceId(inv.getInvoiceId())
                .orderId(p.getOrderId())
                .paymentKey(p.getPaymentKey())
                .status(p.getStatus().name())
                .approvedAt(p.getApprovedAt() != null ? p.getApprovedAt().toInstant().toString() : null)
                .method(p.getMethod())
                .receiptUrl(p.getReceiptUrl())
                .build();
    }
}
