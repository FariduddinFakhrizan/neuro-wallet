package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Payment Request Repository
 * Provides database access for PaymentRequest entities
 */
@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    /**
     * Find payment requests by requester
     */
    List<PaymentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    /**
     * Find payment requests by status
     */
    List<PaymentRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId,
            PaymentRequest.RequestStatus status);

    /**
     * Find payment request by ID and status
     */
    Optional<PaymentRequest> findByIdAndStatus(Long id, PaymentRequest.RequestStatus status);
}
