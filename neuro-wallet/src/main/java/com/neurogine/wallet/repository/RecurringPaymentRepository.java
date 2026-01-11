package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.RecurringPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Recurring Payment Repository
 * Provides database access for RecurringPayment entities
 */
@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    /**
     * Find active recurring payments for a user
     */
    List<RecurringPayment> findByUserIdAndIsActiveOrderByNextPaymentDate(Long userId, Boolean isActive);

    /**
     * Find recurring payments due for processing
     */
    List<RecurringPayment> findByNextPaymentDateBeforeAndIsActive(LocalDate date, Boolean isActive);

    /**
     * Find all recurring payments for a user
     */
    List<RecurringPayment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
