package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Eight user-defined queries of five different kinds.
// JPQL uses CLASS and FIELD names (Customer, accountNumber), not table/column names.
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 1. JPQL + @Param: find one customer by account number
    @Query("SELECT c FROM Customer c WHERE c.accountNumber = :accountNumber")
    Optional<Customer> getByAccountNumber(@Param("accountNumber") String accountNumber);

    // 2a. JPQL COUNT: does this account number already exist? (0 = no)
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.accountNumber = :accountNumber")
    long countAccounts(@Param("accountNumber") String accountNumber);

    // 2b. JPQL COUNT: does this email already exist? (case-insensitive)
    @Query("SELECT COUNT(c) FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    long countEmails(@Param("email") String email);

    // 3. JPQL ORDER BY: all customers sorted by name
    @Query("SELECT c FROM Customer c ORDER BY c.fullName ASC")
    List<Customer> findAllOrdered();

    // 4. JPQL LIKE: case-insensitive name search ("sil" finds "Kamal Silva")
    @Query("SELECT c FROM Customer c WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.fullName ASC")
    List<Customer> searchByName(@Param("keyword") String keyword);

    // 5. JPQL aggregate: sum of all balances (returns null if there are no customers)
    @Query("SELECT SUM(c.balance) FROM Customer c")
    BigDecimal getTotalBalance();

    // 6. Native SQL: real MySQL, uses the TABLE name "customers"
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // 7. UPDATE + @Modifying: subtract money ONLY IF the balance is high enough.
    //    Check + update happen in ONE statement (atomic). Returns rows changed: 1 = ok, 0 = too low / not found.
    //    @Transactional -> joins the transfer's transaction (or runs as a mini-transaction if there is none).
    //    clearAutomatically -> refreshes Hibernate's cache after the bulk update.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance - :amount " +
           "WHERE c.accountNumber = :accountNumber AND c.balance >= :amount")
    int debit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    // 8. UPDATE + @Modifying: add money to an account. Returns rows changed (0 = account not found).
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance + :amount WHERE c.accountNumber = :accountNumber")
    int credit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
