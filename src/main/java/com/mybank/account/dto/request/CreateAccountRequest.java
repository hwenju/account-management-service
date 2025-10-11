package com.mybank.account.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * create account request DTO
 *
 */
@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account number cannot be blank")
    @Size(min = 2, max = 20, message = "Account number must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Account number can only contain uppercase letters and digits")
    private String accountNumber;

    @NotBlank(message = "Account holder name cannot be blank")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolderName;

    @Email(message = "Email format is invalid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^[0-9\\-+()\\s]*$", message = "Phone number format is invalid")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Balance format is invalid")
    private BigDecimal initialBalance = BigDecimal.ZERO;
}