package com.mybank.account.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * response DTO
 */
@Data
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private String phone;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}