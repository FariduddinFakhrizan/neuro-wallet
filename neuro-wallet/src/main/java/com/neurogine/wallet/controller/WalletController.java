package com.neurogine.wallet.controller;

import com.neurogine.wallet.entity.*;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.UserRepository;
import com.neurogine.wallet.repository.WalletRepository;
import com.neurogine.wallet.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wallet Controller
 * Complete wallet operations including transactions, recurring payments,
 * payment requests, limits, and approval workflows
 */
@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "http://localhost:5173")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionLimitService limitService;

    @Autowired
    private RecurringPaymentService recurringPaymentService;

    @Autowired
    private PaymentRequestService paymentRequestService;

    @Autowired
    private ApprovalService approvalService;

    // ================= WALLET & BALANCE =================

    /**
     * Get wallet balance
     * GET /api/wallet/balance/{userId}
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<Double> getBalance(@PathVariable Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return ResponseEntity.ok(wallet.getBalance());
    }

    // ================= TRANSACTIONS =================

    /**
     * Get transaction history
     * GET /api/wallet/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Transaction>> getHistory(@PathVariable Long userId) {
        List<Transaction> transactions = transactionService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transactions filtered by category
     * GET /api/wallet/transactions?userId={userId}&category={category}
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsByCategory(
            @RequestParam Long userId,
            @RequestParam(required = false) Transaction.TransactionCategory category) {

        List<Transaction> transactions = category != null
                ? transactionService.getTransactionsByCategory(userId, category)
                : transactionService.getTransactionHistory(userId);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Transfer money (single transfer with notes & category)
     * POST /api/wallet/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> payload) {
        Long fromUserId = ((Number) payload.get("fromUserId")).longValue();
        Double amount = ((Number) payload.get("amount")).doubleValue();

        // Support both userId and username for recipient
        Long toUserId = null;
        if (payload.containsKey("toUserId")) {
            toUserId = ((Number) payload.get("toUserId")).longValue();
        } else if (payload.containsKey("toUsername")) {
            String username = (String) payload.get("toUsername");
            toUserId = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                    .getId();
        }

        // Get category and note
        Transaction.TransactionCategory category = null;
        if (payload.containsKey("category")) {
            category = Transaction.TransactionCategory.valueOf((String) payload.get("category"));
        }
        String note = (String) payload.get("note");

        // Check if requires approval
        Long approverId = payload.containsKey("approverId")
                ? ((Number) payload.get("approverId")).longValue()
                : null;

        if (approvalService.requiresApproval(amount) && approverId != null) {
            // Create pending approval
            PendingApproval approval = approvalService.createPendingApproval(
                    fromUserId, toUserId, amount, approverId, category, note);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transaction requires approval");
            response.put("requiresApproval", true);
            response.put("approvalId", approval.getId());
            response.put("threshold", 1000.0);

            return ResponseEntity.ok(response);
        }

        // Create transaction directly
        Transaction transaction = transactionService.createTransaction(fromUserId, toUserId, amount, category, note);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transfer successful");
        response.put("transaction", transaction);

        return ResponseEntity.ok(response);
    }

    /**
     * Batch transfer
     * POST /api/wallet/batch-transfer
     */
    @PostMapping("/batch-transfer")
    public ResponseEntity<Map<String, Object>> batchTransfer(@RequestBody Map<String, Object> payload) {
        Long fromUserId = ((Number) payload.get("fromUserId")).longValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recipients = (List<Map<String, Object>>) payload.get("recipients");

        Transaction.TransactionCategory category = null;
        if (payload.containsKey("category")) {
            category = Transaction.TransactionCategory.valueOf((String) payload.get("category"));
        }
        String note = (String) payload.get("note");

        Map<String, Object> result = transactionService.batchTransfer(fromUserId, recipients, category, note);

        return ResponseEntity.ok(result);
    }

    // ================= TRANSACTION LIMITS =================

    /**
     * Get transaction limits
     * GET /api/wallet/limits/{userId}
     */
    @GetMapping("/limits/{userId}")
    public ResponseEntity<TransactionLimit> getLimits(@PathVariable Long userId) {
        TransactionLimit limit = limitService.getLimit(userId);
        return ResponseEntity.ok(limit);
    }

    /**
     * Update transaction limits
     * PUT /api/wallet/limits/{userId}
     */
    @PutMapping("/limits/{userId}")
    public ResponseEntity<TransactionLimit> updateLimits(
            @PathVariable Long userId,
            @RequestBody Map<String, Double> payload) {

        Double dailyLimit = payload.get("dailyLimit");
        Double weeklyLimit = payload.get("weeklyLimit");
        Double monthlyLimit = payload.get("monthlyLimit");

        TransactionLimit limit = limitService.updateLimits(userId, dailyLimit, weeklyLimit, monthlyLimit);

        return ResponseEntity.ok(limit);
    }

    // ================= RECURRING PAYMENTS =================

    /**
     * Get recurring payments for user
     * GET /api/wallet/recurring/{userId}
     */
    @GetMapping("/recurring/{userId}")
    public ResponseEntity<List<RecurringPayment>> getRecurringPayments(@PathVariable Long userId) {
        List<RecurringPayment> payments = recurringPaymentService.getUserRecurringPayments(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Create recurring payment
     * POST /api/wallet/recurring
     */
    @PostMapping("/recurring")
    public ResponseEntity<RecurringPayment> createRecurringPayment(@RequestBody Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        Long recipientId = ((Number) payload.get("recipientId")).longValue();
        Double amount = ((Number) payload.get("amount")).doubleValue();
        RecurringPayment.PaymentFrequency frequency = RecurringPayment.PaymentFrequency.valueOf(
                (String) payload.get("frequency"));

        Transaction.TransactionCategory category = null;
        if (payload.containsKey("category")) {
            category = Transaction.TransactionCategory.valueOf((String) payload.get("category"));
        }
        String note = (String) payload.get("note");

        RecurringPayment payment = recurringPaymentService.createRecurringPayment(
                userId, recipientId, amount, frequency, category, note);

        return ResponseEntity.ok(payment);
    }

    /**
     * Cancel recurring payment
     * DELETE /api/wallet/recurring/{id}
     */
    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<Map<String, String>> cancelRecurringPayment(@PathVariable Long id) {
        recurringPaymentService.cancelRecurringPayment(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Recurring payment cancelled successfully");

        return ResponseEntity.ok(response);
    }

    // ================= PAYMENT REQUESTS =================

    /**
     * Create payment request
     * POST /api/wallet/payment-request
     */
    @PostMapping("/payment-request")
    public ResponseEntity<PaymentRequest> createPaymentRequest(@RequestBody Map<String, Object> payload) {
        Long requesterId = ((Number) payload.get("requesterId")).longValue();
        Double amount = ((Number) payload.get("amount")).doubleValue();
        String note = (String) payload.get("note");
        Integer expiryHours = payload.containsKey("expiryHours")
                ? ((Number) payload.get("expiryHours")).intValue()
                : 24;

        PaymentRequest request = paymentRequestService.createPaymentRequest(requesterId, amount, note, expiryHours);

        return ResponseEntity.ok(request);
    }

    /**
     * Get payment requests for user
     * GET /api/wallet/payment-requests/{userId}
     */
    @GetMapping("/payment-requests/{userId}")
    public ResponseEntity<List<PaymentRequest>> getPaymentRequests(@PathVariable Long userId) {
        List<PaymentRequest> requests = paymentRequestService.getUserPaymentRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Pay a payment request
     * POST /api/wallet/payment-request/{id}/pay
     */
    @PostMapping("/payment-request/{id}/pay")
    public ResponseEntity<Map<String, Object>> payRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        Long payerId = ((Number) payload.get("payerId")).longValue();
        Transaction transaction = paymentRequestService.payRequest(id, payerId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payment request paid successfully");
        response.put("transaction", transaction);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel payment request
     * DELETE /api/wallet/payment-request/{id}
     */
    @DeleteMapping("/payment-request/{id}")
    public ResponseEntity<Map<String, String>> cancelPaymentRequest(
            @PathVariable Long id,
            @RequestParam Long userId) {

        paymentRequestService.cancelRequest(id, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Payment request cancelled successfully");

        return ResponseEntity.ok(response);
    }

    // ================= PENDING APPROVALS =================

    /**
     * Get pending approvals for approver
     * GET /api/wallet/pending-approvals/{approverId}
     */
    @GetMapping("/pending-approvals/{approverId}")
    public ResponseEntity<List<PendingApproval>> getPendingApprovals(@PathVariable Long approverId) {
        List<PendingApproval> approvals = approvalService.getPendingApprovals(approverId);
        return ResponseEntity.ok(approvals);
    }

    /**
     * Get all approval requests made by user
     * GET /api/wallet/approval-requests/{userId}
     */
    @GetMapping("/approval-requests/{userId}")
    public ResponseEntity<List<PendingApproval>> getApprovalRequests(@PathVariable Long userId) {
        List<PendingApproval> approvals = approvalService.getUserApprovalRequests(userId);
        return ResponseEntity.ok(approvals);
    }

    /**
     * Approve transaction
     * POST /api/wallet/pending-approvals/{id}/approve
     */
    @PostMapping("/pending-approvals/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveTransaction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        Long approverId = ((Number) payload.get("approverId")).longValue();
        Transaction transaction = approvalService.approveTransaction(id, approverId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transaction approved and executed successfully");
        response.put("transaction", transaction);

        return ResponseEntity.ok(response);
    }

    /**
     * Reject transaction
     * POST /api/wallet/pending-approvals/{id}/reject
     */
    @PostMapping("/pending-approvals/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectTransaction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        Long approverId = ((Number) payload.get("approverId")).longValue();
        approvalService.rejectTransaction(id, approverId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Transaction rejected successfully");

        return ResponseEntity.ok(response);
    }
}
