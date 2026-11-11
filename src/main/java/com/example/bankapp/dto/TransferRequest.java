package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Form object: holds the data typed in the transfer form (a DTO, not an Entity)
@Getter
@Setter
@NoArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Please select the sender account")
    private String fromAccount;

    @NotBlank(message = "Please select the receiver account")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
    private BigDecimal amount;

    // The "Simulate a system failure" checkbox (unticked = false)
    private boolean simulateFailure;
}
