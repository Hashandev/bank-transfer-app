package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Value("${bank.transfer-fee.enabled:false}")
    private boolean feeEnabled;

    @Value("${bank.transfer-fee.account:999999}")
    private String feeAccount;

    @Override
    public void run(String... args) throws Exception {
        // CommandLineRunner -> runs ONCE when the application starts
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    new Customer("100001", "Kamal Silva", "kamal@example.com", new BigDecimal("50000.00")),
                    new Customer("100002", "Nimali Perera", "nimali@example.com", new BigDecimal("25000.00")),
                    new Customer("100003", "Saman Kumara", "saman@example.com", new BigDecimal("10000.00"))
            ));
            System.out.println(">>> 3 sample customers inserted (total LKR 85,000.00)");
        }

        // Task 12: the bank's own fee account (balance 0, so the total money does not change)
        if (feeEnabled && customerRepository.countAccounts(feeAccount) == 0) {
            customerRepository.save(new Customer(feeAccount, "Bank Fee Account",
                    "fees@bank.example.com", new BigDecimal("0.00")));
            System.out.println(">>> Fee account " + feeAccount + " created");
        }
    }
}
