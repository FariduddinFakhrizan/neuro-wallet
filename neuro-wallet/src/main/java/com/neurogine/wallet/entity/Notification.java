package com.neurogine.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification Entity
 * System notifications for users
 */
@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "reference_id")
    private Long referenceId; // Transaction ID, Approval ID, etc.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        TRANSACTION_RECEIVED,
        TRANSACTION_SENT,
        LOW_BALANCE,
        LARGE_TRANSACTION,
        APPROVAL_PENDING,
        APPROVAL_APPROVED,
        APPROVAL_REJECTED,
        PAYMENT_REQUEST_RECEIVED,
        BUDGET_ALERT,
        RECURRING_PAYMENT_PROCESSED
    }
}
