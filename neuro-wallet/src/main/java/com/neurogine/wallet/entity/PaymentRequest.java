package com.neurogine.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Payment Request Entity
 * Represents a request for payment that can be paid via QR code
 */
@Entity
@Table(name = "payment_requests")
@Data
public class PaymentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Double amount;

    @Column(length = 500)
    private String note;

    @Column(name = "qr_code", length = 1000)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "paid_by")
    private Long paidBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Payment Request Status
     */
    public enum RequestStatus {
        PENDING,
        PAID,
        EXPIRED,
        CANCELLED
    }
}
