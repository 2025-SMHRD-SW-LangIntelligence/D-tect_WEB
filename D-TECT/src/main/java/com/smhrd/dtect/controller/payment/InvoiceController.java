package com.smhrd.dtect.controller.payment;

import com.smhrd.dtect.dto.payment.InvoiceCreateRequest;
import com.smhrd.dtect.dto.payment.InvoiceResponse;
import com.smhrd.dtect.dto.payment.InvoiceSendRequest;
import com.smhrd.dtect.entity.payment.InvoiceStatus;
import com.smhrd.dtect.service.payment.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // 전문가가 청구서 작성 (DRAFT)
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@RequestBody InvoiceCreateRequest req) {
        InvoiceResponse created = invoiceService.create(req);
        return ResponseEntity
                .created(URI.create("/api/invoices/" + created.getInvoiceId()))
                .body(created);
    }

    // 전문가가 청구서 발송 (DRAFT -> SENT). 매칭이 COMPLETED일 때만 성공
    @PostMapping("/{invoiceId}/send")
    public ResponseEntity<InvoiceResponse> send(@PathVariable Long invoiceId,
                                                @RequestBody(required = false) InvoiceSendRequest req) {
        InvoiceResponse sent = invoiceService.send(invoiceId, req);
        return ResponseEntity.ok(sent);
    }

    // 청구서 취소 (PAID는 취소 불가)
    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable Long invoiceId) {
        InvoiceResponse canceled = invoiceService.cancel(invoiceId);
        return ResponseEntity.ok(canceled);
    }

    // 단건 조회
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getOne(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getOne(invoiceId));
    }

    // 특정 사용자에게 보낸 청구서 목록
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<InvoiceResponse>> listForUser(@PathVariable Long userId,
                                                             @RequestParam(required = false) InvoiceStatus status) {
        return ResponseEntity.ok(invoiceService.listForUser(userId, status));
    }

    // 특정 매칭의 청구서 히스토리
    @GetMapping("/matchings/{matchingId}")
    public ResponseEntity<List<InvoiceResponse>> listForMatching(@PathVariable Long matchingId) {
        return ResponseEntity.ok(invoiceService.listForMatching(matchingId));
    }
}
