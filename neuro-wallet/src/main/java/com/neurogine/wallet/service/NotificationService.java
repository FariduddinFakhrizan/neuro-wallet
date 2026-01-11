package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.Notification;
import com.neurogine.wallet.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification Service
 * Manages system notifications
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a notification
     */
    @Transactional
    public Notification createNotification(Long userId, Notification.NotificationType type,
            String message, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    /**
     * Get unread count
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all as read
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = getUnreadNotifications(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Helper: Notify on transaction received
     */
    public void notifyTransactionReceived(Long userId, Long senderId, Double amount) {
        String message = String.format("You received RM %.2f from User #%d", amount, senderId);
        createNotification(userId, Notification.NotificationType.TRANSACTION_RECEIVED, message, senderId);
    }

    /**
     * Helper: Notify on low balance
     */
    public void notifyLowBalance(Long userId, Double balance, Double threshold) {
        String message = String.format("Low balance alert: Your balance (RM %.2f) is below RM %.2f", balance,
                threshold);
        createNotification(userId, Notification.NotificationType.LOW_BALANCE, message, null);
    }

    /**
     * Helper: Notify on budget alert
     */
    public void notifyBudgetAlert(Long userId, String category, Double spent, Double limit) {
        String message = String.format("Budget alert: You've spent RM %.2f of your RM %.2f %s budget", spent, limit,
                category);
        createNotification(userId, Notification.NotificationType.BUDGET_ALERT, message, null);
    }
}
