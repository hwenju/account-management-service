package com.mybank.account.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * all the business exceptions write in here
 * If more exceptions need to be added in the future, might consider writing some exception subclasses.
 */
@Getter
public class BankException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    private BankException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    private BankException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    // ========== Account Related Exceptions ==========
    public static BankException accountNotFound(String accountNumber) {
        return new BankException(
                String.format("Account not found: %s", accountNumber),
                "ACCOUNT_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static BankException accountNotFound(Long id) {
        return new BankException(
                String.format("Account not found (ID): %d", id),
                "ACCOUNT_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static BankException duplicateAccount(String accountNumber) {
        return new BankException(
                String.format("Account already exists: %s", accountNumber),
                "DUPLICATE_ACCOUNT",
                HttpStatus.CONFLICT
        );
    }

    // ========== Balance Related Exceptions ==========
    public static BankException insufficientBalance(String accountNumber, BigDecimal current, BigDecimal required) {
        return new BankException(
                String.format("Insufficient balance for account %s. Current balance: %.2f, Required amount: %.2f",
                        accountNumber, current, required),
                "INSUFFICIENT_BALANCE",
                HttpStatus.BAD_REQUEST
        );
    }

    public static BankException insufficientBalance(String message) {
        return new BankException(
                message,
                "INSUFFICIENT_BALANCE",
                HttpStatus.BAD_REQUEST
        );
    }

    // ========== Business Rule Exceptions ==========
    public static BankException invalidAmount(String message) {
        return new BankException(
                message,
                "INVALID_AMOUNT",
                HttpStatus.BAD_REQUEST
        );
    }

    public static BankException invalidAccountOperation(String message) {
        return new BankException(
                message,
                "INVALID_OPERATION",
                HttpStatus.BAD_REQUEST
        );
    }

    public static BankException sameAccountTransfer() {
        return new BankException(
                "Cannot transfer to the same account",
                "INVALID_TRANSFER",
                HttpStatus.BAD_REQUEST
        );
    }

    // ========== System Exceptions ==========
    public static BankException concurrencyConflict() {
        return new BankException(
                "Data has been modified by another operation, please try again",
                "CONCURRENCY_CONFLICT",
                HttpStatus.CONFLICT
        );
    }

    public static BankException dataIntegrityError(String message) {
        return new BankException(
                message,
                "DATA_INTEGRITY_ERROR",
                HttpStatus.CONFLICT
        );
    }

    // ========== General Business Exceptions ==========
    public static BankException businessError(String message) {
        return new BankException(
                message,
                "BUSINESS_ERROR",
                HttpStatus.BAD_REQUEST
        );
    }
}