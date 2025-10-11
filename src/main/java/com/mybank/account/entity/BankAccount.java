package com.mybank.account.entity;

import com.mybank.account.exception.BankException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * the bank account entity
 */
@Entity
@Table(name = "bank_account",
        indexes = {
                @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
                @Index(name = "idx_account_holder", columnList = "accountHolderName"),
                @Index(name = "idx_created_at", columnList = "createdAt DESC") // use in sort
        })
@Getter
@Setter
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, updatable = false, unique = true)
    private String accountNumber;

    @NotBlank
    @Column(nullable = false)
    private String accountHolderName;

    private String email;

    private String phone;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * deposit basic operation in the entity
     */
    public void deposit(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * withdraw basic operation in the entity
     */
    public void withdraw(BigDecimal amount) {
        validateAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw BankException.insufficientBalance(
                    String.format("Insufficient balance. Current: %s, Required: %s", balance, amount));
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * validate amount
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public String toString() {
        return String.format("BankAccount{id=%d, accountNumber='%s', holder='%s', balance=%s}",
                id, accountNumber, accountHolderName, balance);
    }
}