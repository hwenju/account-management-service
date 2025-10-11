package com.mybank.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * update account request DTO
 */
@Data
public class UpdateAccountRequest {

    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolderName;

    @Email(message = "Email format is incorrect")
    @Size(max = 100, message = "Email length cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^[0-9\\-+()\\s]*$", message = "Phone number format is incorrect")
    @Size(max = 20, message = "Phone number length cannot exceed 20 characters")
    private String phone;
}