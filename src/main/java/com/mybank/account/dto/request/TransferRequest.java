package com.mybank.account.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * transfer request DTO
 */
@Data
public class TransferRequest {

    @NotBlank(message = "From account number cannot be blank")
    private String fromAccountNumber;

    @NotBlank(message = "To account number cannot be blank")
    private String toAccountNumber;

    @NotNull(message = "Transfer amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Amount format is incorrect")
    private BigDecimal amount;

    // can write remark
    @Size(max = 200, message = "Remark length cannot exceed 200 characters")
    private String remark;
}