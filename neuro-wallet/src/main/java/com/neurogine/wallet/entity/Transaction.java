package com.neurogine.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Transaction Entity
 * Represents a financial transaction between users with enhanced features
 * Supports categories, notes, and approval workflows
 */
@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category = TransactionCategory.OTHER;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime timestamp;

    /**
     * Transaction Categories
     */
    public enum TransactionCategory {
        RENT,
        FOOD,
        SALARY,
        ENTERTAINMENT,
        UTILITIES,
        SHOPPING,
        HEALTHCARE,
        TRANSPORT,
        EDUCATION,
        OTHER
    }

    /**
     * Transaction Status
     */
    public enum TransactionStatus {
        COMPLETED,
        PENDING_APPROVAL,
        REJECTED,
        CANCELLED
    }
}
