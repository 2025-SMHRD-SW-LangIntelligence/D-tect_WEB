package com.smhrd.dtect.repository.payment;

import com.smhrd.dtect.entity.payment.Invoice;
import com.smhrd.dtect.entity.payment.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByUser_UserIdxAndStatusOrderByCreatedAtDesc(Long userId, InvoiceStatus status);

    List<Invoice> findByMatching_MatchingIdxOrderByCreatedAtDesc(Long matchingId);

    Optional<Invoice> findFirstByMatching_MatchingIdxAndStatusOrderByCreatedAtDesc(Long matchingId, InvoiceStatus status);

    boolean existsByMatching_MatchingIdxAndStatus(Long matchingId, InvoiceStatus status);
}