package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row for EVERY transfer, successful or failed -> table "bank_transactions".
 * Deposits have no sender (fromAccount = null), withdrawals have no receiver (toAccount = null).
 */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account", length = 6)
    private String fromAccount;

    @Column(name = "to_account", length = 6)
    private String toAccount;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    // Save the enum as text ("SUCCESS"/"FAILED"), not as a number (0/1)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Small helper to create a record in one line. */
    public static BankTransaction of(String fromAccount, String toAccount, BigDecimal amount,
                                     TransactionStatus status, String description) {
        BankTransaction t = new BankTransaction();
        t.setFromAccount(fromAccount);
        t.setToAccount(toAccount);
        t.setAmount(amount);
        t.setStatus(status);
        if (description != null && description.length() > 255) {
            description = description.substring(0, 255); // fits the 255-character column
        }
        t.setDescription(description);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}
