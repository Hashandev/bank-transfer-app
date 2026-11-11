package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
// IMPORTANT: Spring's @Transactional (NOT jakarta.transaction.Transactional)
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/*
 * @Transactional(readOnly = true) on the CLASS:
 *   default for every method = read-only transaction (reports, searches).
 *   Methods that WRITE data override it with their own @Transactional.
 *
 * @RequiredArgsConstructor (Lombok):
 *   generates a constructor for the two "final" repositories -> Spring injects them (constructor injection).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Task 12 settings from application.properties
    @Value("${bank.transfer-fee.enabled:false}")
    private boolean feeEnabled;

    @Value("${bank.transfer-fee.rate:0.01}")
    private BigDecimal feeRate;

    @Value("${bank.transfer-fee.account:999999}")
    private String feeAccount;

    // =====================================================================
    //  THE HEART OF THIS LAB
    // =====================================================================
    /*
     * rollbackFor = Exception.class -> read-write transaction that rolls back for ANY exception
     *                                  (RuntimeException AND checked exceptions).
     *
     * Spring proxy:  BEGIN -> step 1..5 -> no exception = COMMIT (all saved)
     *                                   -> exception    = ROLLBACK (all undone)
     *
     * Do NOT catch the exception inside this method. If we catch it and don't rethrow,
     * Spring never sees the failure and COMMITS the half-finished work.
     */
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount,
                                    BigDecimal amount, boolean simulateFailure) {

        // ---- Step 1: Validate (accounts different and both exist) ----
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than 0");
        }
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        if (customerRepository.countAccounts(fromAccount) == 0) {
            throw new AccountNotFoundException(fromAccount);
        }
        if (customerRepository.countAccounts(toAccount) == 0) {
            throw new AccountNotFoundException(toAccount);
        }

        // Task 12: 1% fee (only when bank.transfer-fee.enabled=true)
        BigDecimal fee = BigDecimal.ZERO;
        if (feeEnabled) {
            fee = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalToWithdraw = amount.add(fee);

        // ---- Step 2: debit(sender) -> UPDATE ... WHERE balance >= amount ----
        int rows = customerRepository.debit(fromAccount, totalToWithdraw);
        if (rows == 0) {
            throw new InsufficientBalanceException(fromAccount); // -> ROLLBACK
        }

        // ---- Step 3: (Experiment) simulated crash AFTER the withdrawal ----
        if (simulateFailure) {
            throw new BankException(
                    "Simulated system failure after the withdrawal. All changes were rolled back.");
        }

        // ---- Step 4: credit(receiver) -> UPDATE ----
        rows = customerRepository.credit(toAccount, amount);
        if (rows == 0) {
            throw new AccountNotFoundException(toAccount);
        }

        // Task 12: the fee goes to bank account 999999 (still the SAME transaction = 3 updates)
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            rows = customerRepository.credit(feeAccount, fee);
            if (rows == 0) {
                throw new AccountNotFoundException(feeAccount);
            }
            transactionRepository.save(BankTransaction.of(fromAccount, feeAccount, fee,
                    TransactionStatus.SUCCESS, "Transfer fee (1%)"));
        }

        // ---- Step 5: save a SUCCESS record -> INSERT ----
        return transactionRepository.save(BankTransaction.of(fromAccount, toAccount, amount,
                TransactionStatus.SUCCESS, "Transfer completed"));
    }

    // =====================================================================
    //  Write methods (read-write transaction)
    // =====================================================================

    @Transactional
    public Customer createCustomer(Customer customer) {
        customer.setAccountNumber(customer.getAccountNumber().trim());
        customer.setFullName(customer.getFullName().trim());
        customer.setEmail(customer.getEmail().trim());

        // Custom @Query checks (COUNT)
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number " + customer.getAccountNumber() + " already exists");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email " + customer.getEmail() + " already exists");
        }
        customer.setId(null); // always INSERT a new row
        return customerRepository.save(customer);
    }

    // Task 12: deposit uses credit()
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        return transactionRepository.save(BankTransaction.of(null, accountNumber, amount,
                TransactionStatus.SUCCESS, "Cash deposit"));
    }

    // Task 12: withdraw uses debit()
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        if (customerRepository.countAccounts(accountNumber) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        return transactionRepository.save(BankTransaction.of(accountNumber, null, amount,
                TransactionStatus.SUCCESS, "Cash withdrawal"));
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than 0");
        }
    }

    // =====================================================================
    //  Read methods (use the class default: readOnly = true)
    // =====================================================================

    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total; // SUM returns null when there are no rows
    }

    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return transactionRepository.findLargeTransfers(min, TransactionStatus.SUCCESS);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.getTotalSent(accountNumber, TransactionStatus.SUCCESS);
        return total == null ? BigDecimal.ZERO : total;
    }

    public boolean isFeeEnabled() {
        return feeEnabled;
    }
}