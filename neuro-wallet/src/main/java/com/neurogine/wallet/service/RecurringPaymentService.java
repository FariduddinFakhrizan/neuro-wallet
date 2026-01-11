package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.RecurringPayment;
import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.RecurringPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Recurring Payment Service
 * Manages automated recurring payments
 */
@Service
public class RecurringPaymentService {

    @Autowired
    private RecurringPaymentRepository recurringPaymentRepository;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create a new recurring payment
     */
    @Transactional
    public RecurringPayment createRecurringPayment(Long userId, Long recipientId, Double amount,
            RecurringPayment.PaymentFrequency frequency,
            Transaction.TransactionCategory category, String note) {
        RecurringPayment payment = new RecurringPayment();
        payment.setUserId(userId);
        payment.setRecipientId(recipientId);
        payment.setAmount(amount);
        payment.setFrequency(frequency);
        payment.setCategory(category != null ? category : Transaction.TransactionCategory.OTHER);
        payment.setNote(note);
        payment.setIsActive(true);
        payment.setNextPaymentDate(calculateNextPaymentDate(LocalDate.now(), frequency));

        return recurringPaymentRepository.save(payment);
    }

    /**
     * Cancel a recurring payment
     */
    @Transactional
    public void cancelRecurringPayment(Long id) {
        RecurringPayment payment = recurringPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring payment not found"));

        payment.setIsActive(false);
        recurringPaymentRepository.save(payment);
    }

    /**
     * Get all recurring payments for a user
     */
    public List<RecurringPayment> getUserRecurringPayments(Long userId) {
        return recurringPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get active recurring payments for a user
     */
    public List<RecurringPayment> getActiveRecurringPayments(Long userId) {
        return recurringPaymentRepository.findByUserIdAndIsActiveOrderByNextPaymentDate(userId, true);
    }

    /**
     * Process recurring payments (scheduled task)
     * Runs every minute to check for due payments
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void processRecurringPayments() {
        List<RecurringPayment> duePayments = recurringPaymentRepository
                .findByNextPaymentDateBeforeAndIsActive(LocalDate.now().plusDays(1), true);

        for (RecurringPayment payment : duePayments) {
            try {
                // Execute the payment
                transactionService.createTransaction(
                        payment.getUserId(),
                        payment.getRecipientId(),
                        payment.getAmount(),
                        payment.getCategory(),
                        "Recurring: " + (payment.getNote() != null ? payment.getNote() : ""));

                // Update next payment date
                payment.setNextPaymentDate(
                        calculateNextPaymentDate(payment.getNextPaymentDate(), payment.getFrequency()));
                recurringPaymentRepository.save(payment);

                System.out.println("Processed recurring payment ID: " + payment.getId());
            } catch (Exception e) {
                // Log error but continue processing other payments
                System.err.println("Failed to process recurring payment ID " + payment.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Calculate next payment date based on frequency
     */
    private LocalDate calculateNextPaymentDate(LocalDate currentDate, RecurringPayment.PaymentFrequency frequency) {
        switch (frequency) {
            case DAILY:
                return currentDate.plusDays(1);
            case WEEKLY:
                return currentDate.plusWeeks(1);
            case MONTHLY:
                return currentDate.plusMonths(1);
            default:
                return currentDate.plusMonths(1);
        }
    }
}
