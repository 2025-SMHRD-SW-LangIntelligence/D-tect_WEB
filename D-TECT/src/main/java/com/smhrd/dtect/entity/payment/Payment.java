package com.smhrd.dtect.entity.payment;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // 토스 위젯에 넘기는 주문번호
    @Column(name = "order_id", length = 120, nullable = false, unique = true)
    private String orderId;

    // 토스 승인 후 채워짐
    @Column(name = "payment_key", length = 200, unique = true)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status; // READY, DONE, FAILED, CANCELED

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private PaymentProvider provider;

    // 결제 금액(원)
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "requested_at", nullable = false)
    private Timestamp requestedAt;

    @Column(name = "approved_at")
    private Timestamp approvedAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "method", length = 30)
    private String method;

    @Column(name = "receipt_url", length = 300)
    private String receiptUrl;

    @Column(name = "card_company", length = 40)
    private String cardCompany;

    @Column(name = "card_bin", length = 12)
    private String cardBin;

    @Column(name = "card_last4", length = 8)
    private String cardLast4;

    // 토스 원문 JSON 저장(장애/감사 대비)
    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;


    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
        if (this.requestedAt == null) this.requestedAt = now;
        if (this.status == null) this.status = PaymentStatus.READY;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
