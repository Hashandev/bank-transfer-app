package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. All transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findAllLatest();

    // 2. History of one account: it is the sender OR the receiver
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.fromAccount = :account OR t.toAccount = :account " +
           "ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findHistory(@Param("account") String account);

    // 3. Filter by status - an enum can be passed as a parameter
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    // 4. Number of SUCCESS or FAILED transactions (dashboard statistics)
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    // ---------- Task 12 ----------

    // 5. Successful transfers with an amount greater than :min  (/large-transfers?min=10000)
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.status = :status AND t.amount > :min " +
           "ORDER BY t.amount DESC, t.id DESC")
    List<BankTransaction> findLargeTransfers(@Param("min") BigDecimal min,
                                             @Param("status") TransactionStatus status);

    // 6. Total amount SENT by an account (SUM of SUCCESS rows where fromAccount = :account).
    //    Includes fee rows. Returns null if nothing was sent.
    @Query("SELECT SUM(t.amount) FROM BankTransaction t " +
           "WHERE t.fromAccount = :account AND t.status = :status")
    BigDecimal getTotalSent(@Param("account") String account,
                            @Param("status") TransactionStatus status);
}
