package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.PaymentRequest;
import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.PaymentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Request Service
 * Handles payment requests with QR codes
 */
@Service
public class PaymentRequestService {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create a new payment request
     */
    @Transactional
    public PaymentRequest createPaymentRequest(Long requesterId, Double amount, String note, Integer expiryHours) {
        PaymentRequest request = new PaymentRequest();
        request.setRequesterId(requesterId);
        request.setAmount(amount);
        request.setNote(note);
        request.setStatus(PaymentRequest.RequestStatus.PENDING);

        // Generate QR code data (simple string format for now)
        String qrData = String.format("PAYMENT_REQUEST:%d:%.2f", requesterId, amount);
        request.setQrCode(qrData);

        // Set expiry (default 24 hours)
        int hours = expiryHours != null ? expiryHours : 24;
        request.setExpiresAt(LocalDateTime.now().plusHours(hours));

        return paymentRequestRepository.save(request);
    }

    /**
     * Pay a payment request
     */
    @Transactional
    public Transaction payRequest(Long requestId, Long payerId) {
        PaymentRequest request = paymentRequestRepository
                .findByIdAndStatus(requestId, PaymentRequest.RequestStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found or already processed"));

        // Check if expired
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            request.setStatus(PaymentRequest.RequestStatus.EXPIRED);
            paymentRequestRepository.save(request);
            throw new BadRequestException("Payment request has expired");
        }

        // Cannot pay your own request
        if (request.getRequesterId().equals(payerId)) {
            throw new BadRequestException("Cannot pay your own payment request");
        }

        // Create the transaction
        Transaction transaction = transactionService.createTransaction(
                payerId,
                request.getRequesterId(),
                request.getAmount(),
                Transaction.TransactionCategory.OTHER,
                "Payment request: " + (request.getNote() != null ? request.getNote() : ""));

        // Update request status
        request.setStatus(PaymentRequest.RequestStatus.PAID);
        request.setPaidBy(payerId);
        request.setPaidAt(LocalDateTime.now());
        paymentRequestRepository.save(request);

        return transaction;
    }

    /**
     * Cancel a payment request
     */
    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));

        // Only requester can cancel
        if (!request.getRequesterId().equals(userId)) {
            throw new BadRequestException("Only the requester can cancel this request");
        }

        if (request.getStatus() != PaymentRequest.RequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be cancelled");
        }

        request.setStatus(PaymentRequest.RequestStatus.CANCELLED);
        paymentRequestRepository.save(request);
    }

    /**
     * Get all payment requests for a user
     */
    public List<PaymentRequest> getUserPaymentRequests(Long userId) {
        return paymentRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get payment request by ID
     */
    public PaymentRequest getPaymentRequestById(Long id) {
        return paymentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));
    }
}
