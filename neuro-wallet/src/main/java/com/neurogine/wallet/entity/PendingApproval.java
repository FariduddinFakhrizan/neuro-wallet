package com.neurogine.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Pending Approval Entity
 * Represents a high-value transaction requiring approval
 */
@Entity
@Table(name = "pending_approvals")
@Data
public class PendingApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.TransactionCategory category = Transaction.TransactionCategory.OTHER;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "approval_threshold", nullable = false)
    private Double approvalThreshold = 1000.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Approval Status
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
