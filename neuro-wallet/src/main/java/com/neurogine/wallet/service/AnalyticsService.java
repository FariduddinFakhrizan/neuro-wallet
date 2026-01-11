package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.Transaction;
import com.neurogine.wallet.repository.TransactionRepository;
import com.neurogine.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service
 * Provides spending analytics, reports, and data export
 */
@Service
public class AnalyticsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    /**
     * Get spending breakdown by category
     */
    /**
     * Get analytics summary: spending breakdown, total income, total expenses
     */
    public Map<String, Object> getSpendingByCategory(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByTimestampDesc(userId);

        // Filter by date range and completed status
        List<Transaction> periodTransactions = allTransactions.stream()
                .filter(t -> (startDate == null || t.getTimestamp().isAfter(startDate)))
                .filter(t -> (endDate == null || t.getTimestamp().isBefore(endDate)))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        // Calculate Total Income (Received)
        double totalIncome = periodTransactions.stream()
                .filter(t -> t.getReceiverId().equals(userId))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Calculate Total Expenses (Sent)
        List<Transaction> expensesInfo = periodTransactions.stream()
                .filter(t -> t.getSenderId().equals(userId))
                .collect(Collectors.toList());

        double totalExpenses = expensesInfo.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Group expenses by category
        Map<Transaction.TransactionCategory, Double> categorySpending = expensesInfo.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)));

        // Convert breakdown to response format
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Map.Entry<Transaction.TransactionCategory, Double> entry : categorySpending.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", entry.getKey().toString());
            item.put("amount", entry.getValue());
            item.put("percentage", totalExpenses > 0 ? (entry.getValue() / totalExpenses * 100) : 0);
            item.put("transactionCount", expensesInfo.stream()
                    .filter(t -> t.getCategory() == entry.getKey())
                    .count());
            breakdown.add(item);
        }

        // Sort breakdown by amount descending
        breakdown.sort((a, b) -> Double.compare((Double) b.get("amount"), (Double) a.get("amount")));

        Map<String, Object> result = new HashMap<>();
        result.put("breakdown", breakdown); // Kept for backward compatibility if needed, but frontend uses
                                            // categoryBreakdown map structure in one place, wait.
        // Actually frontend code provided in step 232/247 uses:
        // v-for="(amount, category) in spendingData.categoryBreakdown"
        // Wait, the frontend code in step 232 line 159 says:
        // v-for="(amount, category) in spendingData.categoryBreakdown"
        // But my previous java code (step 234) returned a LIST of maps under key
        // "breakdown".
        // line 52: List<Map<String, Object>> breakdown = new ArrayList<>();
        // line 68: result.put("breakdown", breakdown);

        // Let's check the frontend code in step 247 (the latest one):
        // line 161: <div v-for="(amount, category) in spendingData.categoryBreakdown"
        // This implies spendingData.categoryBreakdown is an Object/Map where
        // key=category_name, value=amount.

        // BUT, looking at the ORIGINAL Analytics.vue (step 232), it had:
        // line 158: <div v-if="spendingData.categoryBreakdown &&
        // Object.keys(spendingData.categoryBreakdown).length > 0"

        // AND the previous Java code (step 234) returned:
        // List<Map<...>> breakdown
        // result.put("breakdown", breakdown)

        // This is a mismatch! The frontend expects a Map<String, Number> for
        // categoryBreakdown?
        // Let's re-read the frontend code carefully.
        // v-for="(amount, category) in spendingData.categoryBreakdown"
        // If it was a list, it would be v-for="item in spendingData.categoryBreakdown".
        // The pattern (v, k) in object suggests it expects an object.

        // HOWEVER, the Java code in step 234 was returning a LIST under key
        // "breakdown".
        // And the frontend calls `res.data`.
        // If the java returns { "breakdown": [...] }, then
        // `spendingData.value.breakdown` is the list.
        // The frontend uses `spendingData.categoryBreakdown`.

        // So the frontend code provided in the context might ALREADY be broken or I
        // misread it.
        // Let's look at Step 232 (Analytics.vue) again.
        // Line 159: v-for="(amount, category) in spendingData.categoryBreakdown"

        // If I want to fix this, I should make the backend return what the frontend
        // expects.
        // Frontend expects `categoryBreakdown` to be a Map<String, Double>.

        // So I will change the Java code to return `categoryBreakdown` as a Map<String,
        // Double>.

        Map<String, Double> simpleBreakdown = new HashMap<>();
        categorySpending.forEach((k, v) -> simpleBreakdown.put(k.toString(), v));
        result.put("categoryBreakdown", simpleBreakdown);

        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("transactionCount", periodTransactions.size());

        return result;
    }

    /**
     * Get balance history over time
     */
    public List<Map<String, Object>> getBalanceHistory(Long userId, Integer days) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(userId);

        // Get current balance
        Double currentBalance = walletRepository.findByUserId(userId)
                .map(w -> w.getBalance())
                .orElse(0.0);

        // Calculate historical balances
        List<Map<String, Object>> history = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // Work backwards from current balance
        double balance = currentBalance;

        for (LocalDate date = endDate; !date.isBefore(startDate); date = date.minusDays(1)) {
            final LocalDate currentDate = date;

            // Get transactions for this day
            List<Transaction> dayTransactions = transactions.stream()
                    .filter(t -> t.getTimestamp().toLocalDate().equals(currentDate))
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                    .collect(Collectors.toList());

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", currentDate.toString());
            dataPoint.put("balance", Math.round(balance * 100.0) / 100.0);

            history.add(0, dataPoint); // Add at beginning for chronological order

            // Reverse the transactions for this day (go back in time)
            for (Transaction t : dayTransactions) {
                if (t.getSenderId().equals(userId)) {
                    balance += t.getAmount(); // Add back what was sent
                } else if (t.getReceiverId().equals(userId)) {
                    balance -= t.getAmount(); // Subtract what was received
                }
            }
        }

        return history;
    }

    /**
     * Get monthly report
     */
    public Map<String, Object> getMonthlyReport(Long userId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(userId);

        List<Transaction> monthTransactions = transactions.stream()
                .filter(t -> t.getTimestamp().isAfter(startDate) && t.getTimestamp().isBefore(endDate))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        // Calculate income and expenses
        double income = monthTransactions.stream()
                .filter(t -> t.getReceiverId().equals(userId))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expenses = monthTransactions.stream()
                .filter(t -> t.getSenderId().equals(userId))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Get spending by category
        Map<String, Object> categoryBreakdown = getSpendingByCategory(userId, startDate, endDate);

        Map<String, Object> report = new HashMap<>();
        report.put("month", month);
        report.put("year", year);
        report.put("income", income);
        report.put("expenses", expenses);
        report.put("netChange", income - expenses);
        report.put("transactionCount", monthTransactions.size());
        report.put("categoryBreakdown", categoryBreakdown.get("breakdown"));

        return report;
    }

    /**
     * Export transactions to CSV format
     */
    public String exportToCSV(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Category,Amount,Recipient/Sender,Note,Status\n");

        for (Transaction t : transactions) {
            csv.append(t.getTimestamp().toString()).append(",");
            csv.append(t.getSenderId().equals(userId) ? "SENT" : "RECEIVED").append(",");
            csv.append(t.getCategory()).append(",");
            csv.append(t.getAmount()).append(",");
            csv.append(t.getSenderId().equals(userId) ? t.getReceiverId() : t.getSenderId()).append(",");
            csv.append(t.getNote() != null ? "\"" + t.getNote().replace("\"", "\"\"") + "\"" : "").append(",");
            csv.append(t.getStatus()).append("\n");
        }

        return csv.toString();
    }
}
