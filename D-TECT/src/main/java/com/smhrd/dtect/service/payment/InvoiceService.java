package com.smhrd.dtect.service.payment;

import com.smhrd.dtect.dto.payment.InvoiceCreateRequest;
import com.smhrd.dtect.dto.payment.InvoiceResponse;
import com.smhrd.dtect.dto.payment.InvoiceSendRequest;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.entity.payment.Invoice;
import com.smhrd.dtect.entity.payment.InvoiceStatus;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.payment.InvoiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final MatchingRepository matchingRepository;

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest req) {
        if (req.getMatchingId() == null) {
            throw new IllegalArgumentException("matchingId는 필수입니다.");
        }
        if (req.getAmountTotal() == null || req.getAmountTotal() <= 0) {
            throw new IllegalArgumentException("amountTotal은 0보다 커야 합니다.");
        }

        Matching m = matchingRepository.findById(req.getMatchingId())
                .orElseThrow(() -> new IllegalArgumentException("매칭을 찾을 수 없습니다. id=" + req.getMatchingId()));

        if (m.getStatus() == MatchingStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 매칭입니다. 청구서를 생성할 수 없습니다.");
        }

        boolean hasSent = invoiceRepository.existsByMatching_MatchingIdxAndStatus(m.getMatchingIdx(), InvoiceStatus.SENT);
        boolean hasPaid = invoiceRepository.existsByMatching_MatchingIdxAndStatus(m.getMatchingIdx(), InvoiceStatus.PAID);
        if (hasSent || hasPaid) {
            throw new IllegalStateException("이미 발송되었거나 결제된 청구서가 존재합니다.");
        }

        Expert expert = m.getExpert();
        User user     = m.getUser();

        Invoice inv = Invoice.builder()
                .matching(m)
                .expert(expert)
                .user(user)
                .title((req.getTitle() == null || req.getTitle().isBlank()) ? "상담료 청구서" : req.getTitle().trim())
                .description(req.getDescription())
                .amountTotal(req.getAmountTotal())
                .currency("KRW")
                .status(InvoiceStatus.DRAFT)
                .build();

        // dueAt(옵션)
        Timestamp due = parseOptionalTimestamp(req.getDueAt());
        if (due != null) inv.setDueAt(due);

        if (expert != null) inv.setCreatedBy(expert.getMember());

        Invoice saved = invoiceRepository.save(inv);
        return toDto(saved);
    }

    /**
     * 발송: 매칭이 COMPLETED 상태일 때만 가능.
     * DRAFT → SENT (idempotent: 이미 SENT면 그대로 반환)
     */
    @Transactional
    public InvoiceResponse send(Long invoiceId, InvoiceSendRequest req) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("청구서를 찾을 수 없습니다. id=" + invoiceId));

        if (inv.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 청구서입니다.");
        }
        if (inv.getStatus() == InvoiceStatus.CANCELED) {
            throw new IllegalStateException("취소된 청구서는 발송할 수 없습니다.");
        }

        // 같은 매칭에 이미 SENT 상태 청구서가 있으면 차단
        invoiceRepository.findFirstByMatching_MatchingIdxAndStatusOrderByCreatedAtDesc(
                inv.getMatching().getMatchingIdx(), InvoiceStatus.SENT
        ).ifPresent(other -> {
            if (!other.getInvoiceId().equals(inv.getInvoiceId())) {
                throw new IllegalStateException("이미 발송(SENT)된 청구서가 있습니다. 기존 청구서를 결제/취소 후 다시 발송하세요.");
            }
        });

        // 매칭 완료일 때만 발송
        if (inv.getMatching().getStatus() != MatchingStatus.COMPLETED) {
            throw new IllegalStateException("매칭이 완료된 경우에만 청구서를 발송할 수 있습니다.");
        }

        // dueAt 업데이트
        Timestamp newDue = parseOptionalTimestamp(req != null ? req.getDueAt() : null);
        if (newDue != null) inv.setDueAt(newDue);

        if (inv.getStatus() == InvoiceStatus.SENT) {
            return toDto(inv);
        }

        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssuedAt(nowTs());
        return toDto(inv);
    }

    @Transactional
    public InvoiceResponse cancel(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("청구서를 찾을 수 없습니다. id=" + invoiceId));

        if (inv.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 청구서는 취소할 수 없습니다.");
        }
        if (inv.getStatus() == InvoiceStatus.CANCELED) {
            return toDto(inv);
        }

        inv.setStatus(InvoiceStatus.CANCELED);
        inv.setCanceledAt(nowTs());
        return toDto(inv);
    }

    @Transactional
    public InvoiceResponse getOne(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("청구서를 찾을 수 없습니다. id=" + invoiceId));
        return toDto(inv);
    }

    @Transactional
    public List<InvoiceResponse> listForUser(Long userId, InvoiceStatus status) {
        List<Invoice> list = (status != null)
                ? invoiceRepository.findByUser_UserIdxAndStatusOrderByCreatedAtDesc(userId, status)
                : invoiceRepository.findByUser_UserIdxAndStatusOrderByCreatedAtDesc(userId, InvoiceStatus.SENT);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public List<InvoiceResponse> listForMatching(Long matchingId) {
        return invoiceRepository.findByMatching_MatchingIdxOrderByCreatedAtDesc(matchingId)
                .stream().map(this::toDto).toList();
    }


    private static Timestamp parseOptionalTimestamp(String s) {
        if (s == null || s.isBlank()) return null;

        if (s.matches("^\\d{10,}$")) {
            try {
                long ms = Long.parseLong(s);
                return new Timestamp(ms);
            } catch (NumberFormatException ignore) {}
        }

        try {
            return Timestamp.from(Instant.parse(s));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 날짜 형식입니다. (ISO-8601 또는 epoch-ms 문자열 허용): " + s);
        }
    }

    private static Timestamp nowTs() {
        return new Timestamp(System.currentTimeMillis());
    }

    private InvoiceResponse toDto(Invoice e) {
        return InvoiceResponse.builder()
                .invoiceId(e.getInvoiceId())
                .matchingId(e.getMatching() != null ? e.getMatching().getMatchingIdx() : null)
                .expertId(e.getExpert() != null ? e.getExpert().getExpertIdx() : null)
                .userId(e.getUser() != null ? e.getUser().getUserIdx() : null)
                .title(e.getTitle())
                .description(e.getDescription())
                .amountTotal(e.getAmountTotal())
                .currency(e.getCurrency())
                .status(e.getStatus())
                .issuedAt(tsToStr(e.getIssuedAt()))
                .dueAt(tsToStr(e.getDueAt()))
                .paidAt(tsToStr(e.getPaidAt()))
                .canceledAt(tsToStr(e.getCanceledAt()))
                .createdAt(tsToStr(e.getCreatedAt()))
                .updatedAt(tsToStr(e.getUpdatedAt()))
                .build();
    }

    private static String tsToStr(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
