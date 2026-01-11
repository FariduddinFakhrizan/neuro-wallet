package com.neurogine.wallet.controller;

import com.neurogine.wallet.entity.Contact;
import com.neurogine.wallet.entity.Notification;
import com.neurogine.wallet.service.AnalyticsService;
import com.neurogine.wallet.service.ContactService;
import com.neurogine.wallet.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics & Enhanced Features Controller
 * Provides analytics, contacts, and notification endpoints
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private NotificationService notificationService;

    // ================= ANALYTICS =================

    /**
     * Get spending analytics by category
     * GET /api/analytics/spending/{userId}?days=30
     */
    @GetMapping("/analytics/spending/{userId}")
    public ResponseEntity<Map<String, Object>> getSpendingAnalytics(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") Integer days) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        Map<String, Object> analytics = analyticsService.getSpendingByCategory(userId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get balance history
     * GET /api/analytics/balance-history/{userId}?days=30
     */
    @GetMapping("/analytics/balance-history/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getBalanceHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") Integer days) {

        List<Map<String, Object>> history = analyticsService.getBalanceHistory(userId, days);
        return ResponseEntity.ok(history);
    }

    /**
     * Get monthly report
     * GET /api/analytics/monthly-report/{userId}?year=2024&month=1
     */
    @GetMapping("/analytics/monthly-report/{userId}")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @PathVariable Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        Map<String, Object> report = analyticsService.getMonthlyReport(userId, year, month);
        return ResponseEntity.ok(report);
    }

    /**
     * Export transactions to CSV
     * GET /api/analytics/export/{userId}
     */
    @GetMapping("/analytics/export/{userId}")
    public ResponseEntity<String> exportTransactions(@PathVariable Long userId) {
        String csv = analyticsService.exportToCSV(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    // ================= CONTACTS (ADDRESS BOOK) =================

    /**
     * Get all contacts
     * GET /api/contacts/{userId}
     */
    @GetMapping("/contacts/{userId}")
    public ResponseEntity<List<Contact>> getContacts(@PathVariable Long userId) {
        List<Contact> contacts = contactService.getUserContacts(userId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get favorite contacts
     * GET /api/contacts/{userId}/favorites
     */
    @GetMapping("/contacts/{userId}/favorites")
    public ResponseEntity<List<Contact>> getFavoriteContacts(@PathVariable Long userId) {
        List<Contact> contacts = contactService.getFavoriteContacts(userId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Add a contact
     * POST /api/contacts
     */
    @PostMapping("/contacts")
    public ResponseEntity<Contact> addContact(@RequestBody Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        Long contactUserId = ((Number) payload.get("contactUserId")).longValue();
        String nickname = (String) payload.get("nickname");

        Contact contact = contactService.addContact(userId, contactUserId, nickname);
        return ResponseEntity.ok(contact);
    }

    /**
     * Update contact nickname
     * PUT /api/contacts/{id}
     */
    @PutMapping("/contacts/{id}")
    public ResponseEntity<Contact> updateContact(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String nickname = payload.get("nickname");
        Contact contact = contactService.updateContactNickname(id, nickname);
        return ResponseEntity.ok(contact);
    }

    /**
     * Toggle favorite
     * POST /api/contacts/{id}/toggle-favorite
     */
    @PostMapping("/contacts/{id}/toggle-favorite")
    public ResponseEntity<Contact> toggleFavorite(@PathVariable Long id) {
        Contact contact = contactService.toggleFavorite(id);
        return ResponseEntity.ok(contact);
    }

    /**
     * Delete contact
     * DELETE /api/contacts/{id}
     */
    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Map<String, String>> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Contact deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Search contacts
     * GET /api/contacts/{userId}/search?query=john
     */
    @GetMapping("/contacts/{userId}/search")
    public ResponseEntity<List<Contact>> searchContacts(
            @PathVariable Long userId,
            @RequestParam String query) {

        List<Contact> contacts = contactService.searchContacts(userId, query);
        return ResponseEntity.ok(contacts);
    }

    // ================= NOTIFICATIONS =================

    /**
     * Get all notifications
     * GET /api/notifications/{userId}
     */
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications
     * GET /api/notifications/{userId}/unread
     */
    @GetMapping("/notifications/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread count
     * GET /api/notifications/{userId}/unread-count
     */
    @GetMapping("/notifications/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        Long count = notificationService.getUnreadCount(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notification as read
     * POST /api/notifications/{id}/read
     */
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * Mark all as read
     * POST /api/notifications/{userId}/read-all
     */
    @PostMapping("/notifications/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * Delete notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted");
        return ResponseEntity.ok(response);
    }
}
