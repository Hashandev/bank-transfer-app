package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @Entity -> table "customers".
 * This class is ALSO used as the "Add Customer" form object (th:object="${customer}"),
 * so the validation annotations are here too.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor // JPA needs an empty constructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 1, 2, 3 ... automatically
    private Long id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6}", message = "Account number must be exactly 6 digits")
    @Column(name = "account_number", nullable = false, unique = true, length = 6)
    private String accountNumber;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name can have at most 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // BigDecimal = exact money (never double/float). DECIMAL(14,2) in MySQL
    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    public Customer(String accountNumber, String fullName, String email, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.fullName = fullName;
        this.email = email;
        this.balance = balance;
    }
}
