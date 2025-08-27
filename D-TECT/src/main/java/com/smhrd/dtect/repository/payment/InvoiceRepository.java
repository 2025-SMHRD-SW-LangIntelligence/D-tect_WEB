package com.smhrd.dtect.repository.payment;

import com.smhrd.dtect.entity.payment.Invoice;
import com.smhrd.dtect.entity.payment.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	// 1) "Invoice.user (User) → userIdx" 로 조회
    List<Invoice> findByUser_UserIdxAndStatusOrderByCreatedAtDesc(Long userIdx, InvoiceStatus status);

    // 2) 매칭 기준 조회
    List<Invoice> findByMatching_MatchingIdxOrderByCreatedAtDesc(Long matchingIdx);

    Optional<Invoice> findFirstByMatching_MatchingIdxAndStatusOrderByCreatedAtDesc(Long matchingIdx, InvoiceStatus status);

    boolean existsByMatching_MatchingIdxAndStatus(Long matchingIdx, InvoiceStatus status);

    // 3) 작성자(전문가의 Member) 기준이 필요하면 이걸 쓰세요
    List<Invoice> findByCreatedBy_MemIdxOrderByCreatedAtDesc(Long memIdx);
}