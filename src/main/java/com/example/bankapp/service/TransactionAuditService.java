package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/*
 * Saves FAILED records.
 *
 * Why a SEPARATE class?
 *   Calling a @Transactional method from another method of the SAME class bypasses the Spring proxy,
 *   so the transaction settings would be ignored.
 *
 * Why REQUIRES_NEW?
 *   When transfer() fails, its transaction is rolled back. A FAILED record saved in that same
 *   transaction would be erased too. REQUIRES_NEW starts a NEW, independent transaction that
 *   commits on its own -> the failure is always recorded (auditing).
 */
@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String fromAccount, String toAccount, BigDecimal amount, String reason) {
        transactionRepository.save(BankTransaction.of(fromAccount, toAccount, amount,
                TransactionStatus.FAILED, reason));
    }
}
