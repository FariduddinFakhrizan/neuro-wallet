package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Transaction Repository
 * Provides database access for Transaction entities
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a user (sent or received)
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId OR t.receiverId = :userId ORDER BY t.timestamp DESC")
    List<Transaction> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);

    /**
     * Find transactions by sender
     */
    List<Transaction> findBySenderIdOrderByTimestampDesc(Long senderId);

    /**
     * Find transactions by receiver
     */
    List<Transaction> findByReceiverIdOrderByTimestampDesc(Long receiverId);

    /**
     * Find transactions by category
     */
    List<Transaction> findByCategoryOrderByTimestampDesc(Transaction.TransactionCategory category);

    /**
     * Find transactions by status
     */
    List<Transaction> findByStatusOrderByTimestampDesc(Transaction.TransactionStatus status);

    /**
     * Find transactions by user and category
     */
    @Query("SELECT t FROM Transaction t WHERE (t.senderId = :userId OR t.receiverId = :userId) AND t.category = :category ORDER BY t.timestamp DESC")
    List<Transaction> findByUserIdAndCategory(@Param("userId") Long userId,
            @Param("category") Transaction.TransactionCategory category);

    /**
     * Find transactions by user and status
     */
    @Query("SELECT t FROM Transaction t WHERE (t.senderId = :userId OR t.receiverId = :userId) AND t.status = :status ORDER BY t.timestamp DESC")
    List<Transaction> findByUserIdAndStatus(@Param("userId") Long userId,
            @Param("status") Transaction.TransactionStatus status);
}
