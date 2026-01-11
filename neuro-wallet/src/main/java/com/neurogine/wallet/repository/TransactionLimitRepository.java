package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Transaction Limit Repository
 * Provides database access for TransactionLimit entities
 */
@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {

    /**
     * Find transaction limit by user ID
     */
    Optional<TransactionLimit> findByUserId(Long userId);
}
