package com.smhrd.dtect.entity.payment;

import com.smhrd.dtect.entity.expert.Expert;
import com.smhrd.dtect.entity.matching.Matching;
import com.smhrd.dtect.entity.member.Member;
import com.smhrd.dtect.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matching_idx", nullable = false)
    private Matching matching;

    // 조회 최적화용(검색/권한체크)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_idx", nullable = false)
    private Expert expert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    // 작성자(전문가의 Member)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_mem_idx")
    private Member createdBy;

    @Column(name = "title", length = 120, nullable = false)
    private String title; // 예) "상담료 청구서"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 상세 설명

    @Column(name = "amount_total", nullable = false)
    private Integer amountTotal; // KRW 가정

    @Column(name = "currency", length = 10, nullable = false)
    private String currency; // 기본 "KRW"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvoiceStatus status; // DRAFT, SENT, PAID, CANCELED, EXPIRED(선택)

    @Column(name = "issued_at")
    private Timestamp issuedAt;   // SENT 시점

    @Column(name = "due_at")
    private Timestamp dueAt;      // 결제 기한

    @Column(name = "paid_at")
    private Timestamp paidAt;     // PAID 시점

    @Column(name = "canceled_at")
    private Timestamp canceledAt; // 취소 시점

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = InvoiceStatus.DRAFT;
        if (this.currency == null || this.currency.isBlank()) this.currency = "KRW";
        if (this.title == null || this.title.isBlank()) this.title = "상담료 청구서";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
