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
 * Bank account entity representing a customer's bank account.
 * Contains account information, balance, and business operations for deposits and withdrawals.
 * Implements optimistic locking for concurrent access control.
 * 
 * @author Bank Account Service
 * @version 1.0
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
     * Deposits money into this account.
     * Validates amount and updates account balance.
     * 
     * @param amount the amount to deposit (must be positive)
     * @throws IllegalArgumentException if amount is null or not positive
     */
    public void deposit(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Withdraws money from this account.
     * Validates amount and checks sufficient balance before withdrawal.
     * 
     * @param amount the amount to withdraw (must be positive and not exceed balance)
     * @throws IllegalArgumentException if amount is null or not positive
     * @throws BankException if insufficient balance for withdrawal
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
     * Validates that the amount is positive.
     * 
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is null or not positive
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