package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Pending Approval Repository
 * Provides database access for PendingApproval entities
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {

    /**
     * Find approvals pending for a specific approver
     */
    List<PendingApproval> findByApproverIdAndStatusOrderByCreatedAtDesc(Long approverId,
            PendingApproval.ApprovalStatus status);

    /**
     * Find all approvals for an approver
     */
    List<PendingApproval> findByApproverIdOrderByCreatedAtDesc(Long approverId);

    /**
     * Find approvals by requester
     */
    List<PendingApproval> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    /**
     * Find approval by ID and status
     */
    Optional<PendingApproval> findByIdAndStatus(Long id, PendingApproval.ApprovalStatus status);
}
