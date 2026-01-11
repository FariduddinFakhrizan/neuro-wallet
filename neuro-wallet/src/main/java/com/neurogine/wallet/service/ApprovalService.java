package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.PendingApproval;
import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.PendingApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Approval Service
 * Manages multi-signature approval workflow for high-value transactions
 */
@Service
public class ApprovalService {

    @Autowired
    private PendingApprovalRepository approvalRepository;

    @Autowired
    private TransactionService transactionService;

    private static final double DEFAULT_APPROVAL_THRESHOLD = 1000.0;

    /**
     * Check if transaction requires approval
     */
    public boolean requiresApproval(Double amount) {
        return amount >= DEFAULT_APPROVAL_THRESHOLD;
    }

    /**
     * Create pending approval for high-value transaction
     */
    @Transactional
    public PendingApproval createPendingApproval(Long requesterId, Long recipientId, Double amount,
            Long approverId, Transaction.TransactionCategory category, String note) {
        PendingApproval approval = new PendingApproval();
        approval.setRequesterId(requesterId);
        approval.setRecipientId(recipientId);
        approval.setApproverId(approverId);
        approval.setAmount(amount);
        approval.setCategory(category != null ? category : Transaction.TransactionCategory.OTHER);
        approval.setNote(note);
        approval.setStatus(PendingApproval.ApprovalStatus.PENDING);
        approval.setApprovalThreshold(DEFAULT_APPROVAL_THRESHOLD);

        return approvalRepository.save(approval);
    }

    /**
     * Approve a pending transaction
     */
    @Transactional
    public Transaction approveTransaction(Long approvalId, Long approverId) {
        PendingApproval approval = approvalRepository
                .findByIdAndStatus(approvalId, PendingApproval.ApprovalStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending approval not found"));

        // Verify approver
        if (!approval.getApproverId().equals(approverId)) {
            throw new BadRequestException("You are not authorized to approve this transaction");
        }

        // Create the transaction
        Transaction transaction = transactionService.createTransaction(
                approval.getRequesterId(),
                approval.getRecipientId(),
                approval.getAmount(),
                approval.getCategory(),
                approval.getNote());

        // Update approval status
        approval.setStatus(PendingApproval.ApprovalStatus.APPROVED);
        approval.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        return transaction;
    }

    /**
     * Reject a pending transaction
     */
    @Transactional
    public void rejectTransaction(Long approvalId, Long approverId) {
        PendingApproval approval = approvalRepository
                .findByIdAndStatus(approvalId, PendingApproval.ApprovalStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending approval not found"));

        // Verify approver
        if (!approval.getApproverId().equals(approverId)) {
            throw new BadRequestException("You are not authorized to reject this transaction");
        }

        // Update approval status
        approval.setStatus(PendingApproval.ApprovalStatus.REJECTED);
        approval.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(approval);
    }

    /**
     * Get pending approvals for an approver
     */
    public List<PendingApproval> getPendingApprovals(Long approverId) {
        return approvalRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(approverId,
                PendingApproval.ApprovalStatus.PENDING);
    }

    /**
     * Get all approvals for an approver
     */
    public List<PendingApproval> getAllApprovals(Long approverId) {
        return approvalRepository.findByApproverIdOrderByCreatedAtDesc(approverId);
    }

    /**
     * Get approval requests made by a user
     */
    public List<PendingApproval> getUserApprovalRequests(Long userId) {
        return approvalRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
    }
}
