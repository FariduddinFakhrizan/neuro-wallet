package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.entity.Wallet;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.TransactionRepository;
import com.neurogine.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transaction Service
 * Handles all transaction-related business logic
 */
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionLimitService limitService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new transaction
     */
    @Transactional
    public Transaction createTransaction(Long senderId, Long receiverId, Double amount,
            Transaction.TransactionCategory category, String note) {
        // Validate sender and receiver
        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        Wallet receiverWallet = walletRepository.findByUserId(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        // Check balance
        if (senderWallet.getBalance() < amount) {
            throw new BadRequestException("Insufficient balance");
        }

        // Check transaction limits
        limitService.validateAndUpdateLimit(senderId, amount);

        // Perform transfer
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setCategory(category != null ? category : Transaction.TransactionCategory.OTHER);
        transaction.setNote(note);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(transaction);

        // Send notifications
        try {
            notificationService.notifyTransactionReceived(receiverId, senderId, amount);
        } catch (Exception e) {
            // Don't fail transaction if notification fails
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return saved;
    }

    /**
     * Batch transfer to multiple recipients
     */
    @Transactional
    public Map<String, Object> batchTransfer(Long senderId, List<Map<String, Object>> recipients,
            Transaction.TransactionCategory category, String note) {
        // Calculate total amount
        double totalAmount = recipients.stream()
                .mapToDouble(r -> ((Number) r.get("amount")).doubleValue())
                .sum();

        // Validate sender balance
        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        if (senderWallet.getBalance() < totalAmount) {
            throw new BadRequestException("Insufficient balance for batch transfer");
        }

        // Check transaction limits
        limitService.validateAndUpdateLimit(senderId, totalAmount);

        // Process each transfer
        int successCount = 0;
        for (Map<String, Object> recipient : recipients) {
            try {
                Long receiverId = recipient.containsKey("userId")
                        ? ((Number) recipient.get("userId")).longValue()
                        : null;
                Double amount = ((Number) recipient.get("amount")).doubleValue();

                if (receiverId != null && amount > 0) {
                    createTransaction(senderId, receiverId, amount, category, note);
                    successCount++;
                }
            } catch (Exception e) {
                // Log error but continue with other transactions
                System.err.println("Failed to process transfer: " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRecipients", recipients.size());
        result.put("successfulTransfers", successCount);
        result.put("failedTransfers", recipients.size() - successCount);
        result.put("totalAmount", totalAmount);

        return result;
    }

    /**
     * Get transaction history for a user
     */
    public List<Transaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Get transaction history filtered by category
     */
    public List<Transaction> getTransactionsByCategory(Long userId, Transaction.TransactionCategory category) {
        return transactionRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * Get transaction history filtered by status
     */
    public List<Transaction> getTransactionsByStatus(Long userId, Transaction.TransactionStatus status) {
        return transactionRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Get transaction by ID
     */
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }
}
