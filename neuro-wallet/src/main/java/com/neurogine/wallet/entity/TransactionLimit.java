package com.neurogine.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction Limit Entity
 * Tracks spending limits and current spending for a user
 */
@Entity
@Table(name = "transaction_limits")
@Data
public class TransactionLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "daily_limit")
    private Double dailyLimit = 0.0;

    @Column(name = "weekly_limit")
    private Double weeklyLimit = 0.0;

    @Column(name = "monthly_limit")
    private Double monthlyLimit = 0.0;

    @Column(name = "current_daily_spent", nullable = false)
    private Double currentDailySpent = 0.0;

    @Column(name = "current_weekly_spent", nullable = false)
    private Double currentWeeklySpent = 0.0;

    @Column(name = "current_monthly_spent", nullable = false)
    private Double currentMonthlySpent = 0.0;

    @Column(name = "last_daily_reset")
    private LocalDate lastDailyReset;

    @Column(name = "last_weekly_reset")
    private LocalDate lastWeeklyReset;

    @Column(name = "last_monthly_reset")
    private LocalDate lastMonthlyReset;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
