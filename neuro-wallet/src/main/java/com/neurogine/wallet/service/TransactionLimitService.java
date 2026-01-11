package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.entity.TransactionLimit;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.repository.TransactionLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Transaction Limit Service
 * Manages and enforces transaction spending limits
 */
@Service
public class TransactionLimitService {

    @Autowired
    private TransactionLimitRepository limitRepository;

    /**
     * Get or create transaction limit for user
     */
    public TransactionLimit getOrCreateLimit(Long userId) {
        return limitRepository.findByUserId(userId).orElseGet(() -> {
            TransactionLimit limit = new TransactionLimit();
            limit.setUserId(userId);
            limit.setDailyLimit(0.0); // 0 means no limit
            limit.setWeeklyLimit(0.0);
            limit.setMonthlyLimit(0.0);
            limit.setCurrentDailySpent(0.0);
            limit.setCurrentWeeklySpent(0.0);
            limit.setCurrentMonthlySpent(0.0);
            limit.setLastDailyReset(LocalDate.now());
            limit.setLastWeeklyReset(LocalDate.now());
            limit.setLastMonthlyReset(LocalDate.now());
            return limitRepository.save(limit);
        });
    }

    /**
     * Validate and update transaction limit
     */
    @Transactional
    public void validateAndUpdateLimit(Long userId, Double amount) {
        TransactionLimit limit = getOrCreateLimit(userId);

        // Reset counters if needed
        resetIfNeeded(limit);

        // Check daily limit
        if (limit.getDailyLimit() > 0 && limit.getCurrentDailySpent() + amount > limit.getDailyLimit()) {
            throw new BadRequestException(String.format(
                    "Daily limit exceeded. Limit: RM %.2f, Current spent: RM %.2f, Attempting: RM %.2f",
                    limit.getDailyLimit(), limit.getCurrentDailySpent(), amount));
        }

        // Check weekly limit
        if (limit.getWeeklyLimit() > 0 && limit.getCurrentWeeklySpent() + amount > limit.getWeeklyLimit()) {
            throw new BadRequestException(String.format(
                    "Weekly limit exceeded. Limit: RM %.2f, Current spent: RM %.2f, Attempting: RM %.2f",
                    limit.getWeeklyLimit(), limit.getCurrentWeeklySpent(), amount));
        }

        // Check monthly limit
        if (limit.getMonthlyLimit() > 0 && limit.getCurrentMonthlySpent() + amount > limit.getMonthlyLimit()) {
            throw new BadRequestException(String.format(
                    "Monthly limit exceeded. Limit: RM %.2f, Current spent: RM %.2f, Attempting: RM %.2f",
                    limit.getMonthlyLimit(), limit.getCurrentMonthlySpent(), amount));
        }

        // Update spent amounts
        limit.setCurrentDailySpent(limit.getCurrentDailySpent() + amount);
        limit.setCurrentWeeklySpent(limit.getCurrentWeeklySpent() + amount);
        limit.setCurrentMonthlySpent(limit.getCurrentMonthlySpent() + amount);

        limitRepository.save(limit);
    }

    /**
     * Reset counters if time period has passed
     */
    private void resetIfNeeded(TransactionLimit limit) {
        LocalDate now = LocalDate.now();

        // Reset daily if new day
        if (limit.getLastDailyReset() == null || !limit.getLastDailyReset().equals(now)) {
            limit.setCurrentDailySpent(0.0);
            limit.setLastDailyReset(now);
        }

        // Reset weekly if new week
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
        int lastWeek = limit.getLastWeeklyReset() != null
                ? limit.getLastWeeklyReset().get(weekFields.weekOfWeekBasedYear())
                : currentWeek;

        if (currentWeek != lastWeek) {
            limit.setCurrentWeeklySpent(0.0);
            limit.setLastWeeklyReset(now);
        }

        // Reset monthly if new month
        if (limit.getLastMonthlyReset() == null ||
                limit.getLastMonthlyReset().getMonth() != now.getMonth()) {
            limit.setCurrentMonthlySpent(0.0);
            limit.setLastMonthlyReset(now);
        }
    }

    /**
     * Update transaction limits
     */
    @Transactional
    public TransactionLimit updateLimits(Long userId, Double dailyLimit, Double weeklyLimit, Double monthlyLimit) {
        TransactionLimit limit = getOrCreateLimit(userId);

        if (dailyLimit != null)
            limit.setDailyLimit(dailyLimit);
        if (weeklyLimit != null)
            limit.setWeeklyLimit(weeklyLimit);
        if (monthlyLimit != null)
            limit.setMonthlyLimit(monthlyLimit);

        return limitRepository.save(limit);
    }

    /**
     * Get transaction limit for user
     */
    public TransactionLimit getLimit(Long userId) {
        return getOrCreateLimit(userId);
    }
}
