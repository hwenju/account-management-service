package com.mybank.account.service;


import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.dto.request.UpdateAccountRequest;
import com.mybank.account.entity.BankAccount;
import com.mybank.account.exception.BankException;
import com.mybank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service layer for bank account business logic and operations.
 * Handles account CRUD operations, fund transfers, and transaction management
 * with proper concurrency control and caching support.
 * 
 * @author Bank Account Service
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Creates a new bank account with the provided details.
     * Validates account number uniqueness and initializes account balance.
     * 
     * @param request the account creation request
     * @return the created BankAccount entity
     * @throws BankException if account number already exists
     */
    @Transactional
    @CacheEvict(value = {"account", "account-pages"}, allEntries = true)
    public BankAccount createAccount(CreateAccountRequest request) {
        log.info("Creating bank account for holder: {}", request.getAccountHolderName());

        // Verify whether it exists
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw BankException.duplicateAccount(request.getAccountNumber());
        }

        // create account entity
        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setEmail(request.getEmail());
        bankAccount.setPhone(request.getPhone());
        bankAccount.setBalance(request.getInitialBalance());

        BankAccount savedBankAccount = accountRepository.save(bankAccount);
        log.info("Bank account created successfully: {}", savedBankAccount.getAccountNumber());

        return savedBankAccount;
    }

    /**
     * Retrieves all accounts with pagination support.
     * Results are cached for improved performance.
     * 
     * @param pageable pagination and sorting parameters
     * @return Page<BankAccount> containing paginated account list
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "account-pages", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BankAccount> getAllAccounts(Pageable pageable) {
        log.debug("Fetching all accounts with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return accountRepository.findAll(pageable);
    }

    /**
     * Retrieves account details by account number.
     * Uses caching for improved performance.
     * 
     * @param accountNumber the unique account number
     * @return BankAccount entity with account details
     * @throws BankException if account is not found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "account", key = "#accountNumber", unless = "#result == null")
    public BankAccount getAccountByNumber(String accountNumber) {
        log.debug("Fetching account by number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> BankException.accountNotFound(accountNumber));
    }

    /**
     * query account detail by id
     */
    @Transactional(readOnly = true)
    public BankAccount getAccountById(Long id) {
        log.debug("Fetching account by ID: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> BankException.accountNotFound(id));
    }

    /**
     * update account detail
     */
    @Transactional
    @CacheEvict(value = {"account", "account-pages"}, allEntries = true)
    public BankAccount updateAccount(String accountNumber, UpdateAccountRequest request) {
        log.info("update bank account: {}", accountNumber);

        // query and lock account
        BankAccount account = findAndLockAccountByNumber(accountNumber);

        // update the info not null
        if (request.getAccountHolderName() != null) {
            account.setAccountHolderName(request.getAccountHolderName());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }

        BankAccount updatedAccount = accountRepository.save(account);
        log.info("Account updated successfully: {}", accountNumber);

        return updatedAccount;
    }

    /**
     * delete account
     */
    @Transactional
    @CacheEvict(value = {"account", "account-pages"}, allEntries = true)
    public BankAccount deleteAccount(String accountNumber) {
        log.info("Deleting account with accountNumber: {}", accountNumber);

        BankAccount bankAccount = findAndLockAccountByNumber(accountNumber);

        // Verify that the balance is 0 before deleting
        if (bankAccount.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw BankException.insufficientBalance(
                    "Cannot delete account with non-zero balance. Current balance: " + bankAccount.getBalance());
        }

        accountRepository.delete(bankAccount);
        log.info("Account deleted successfully: {}", accountNumber);

        return bankAccount;
    }

    /**
     * Transfers funds between two accounts with concurrency safety.
     * Uses pessimistic locking and deadlock prevention strategies.
     * Implements proper isolation level and transaction timeout.
     * 
     * @param request the transfer request containing source, destination, and amount
     * @throws BankException if accounts don't exist, insufficient funds, or validation fails
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    @CacheEvict(value = {"account", "account-pages"}, allEntries = true)
    public void transferFunds(TransferRequest request) {
        log.info("Initiating fund transfer: {} from {} to {}",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        // Pre-validation
        validateTransferRequest(request);

        // Prevent deadlock: lock by account number order
        String firstAccount, secondAccount;
        if (request.getFromAccountNumber().compareTo(request.getToAccountNumber()) < 0) {
            firstAccount = request.getFromAccountNumber();
            secondAccount = request.getToAccountNumber();
        } else {
            firstAccount = request.getToAccountNumber();
            secondAccount = request.getFromAccountNumber();
        }

        // Lock accounts in order
        BankAccount fromBankAccount = findAndLockAccountByNumber(firstAccount.equals(request.getFromAccountNumber()) ?
                firstAccount : secondAccount);
        BankAccount toBankAccount = findAndLockAccountByNumber(firstAccount.equals(request.getToAccountNumber()) ?
                firstAccount : secondAccount);

        // execute actual transfer
        executeTransfer(fromBankAccount, toBankAccount, request.getAmount());

        log.info("Fund transfer completed successfully. New balances - From: {}, To: {}",
                fromBankAccount.getBalance(), toBankAccount.getBalance());
    }

    /**
     * Validates transfer request parameters.
     * Ensures different source and destination accounts and positive amount.
     * 
     * @param request the transfer request to validate
     * @throws BankException if validation fails
     */
    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw BankException.sameAccountTransfer();
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BankException.invalidAmount("Transfer amount must be positive");
        }
    }

    /**
     * Finds and locks an account by account number for concurrent operations.
     * Uses pessimistic locking to prevent concurrent modifications.
     * 
     * @param accountNumber the account number to find and lock
     * @return BankAccount entity with pessimistic lock
     * @throws BankException if account is not found
     */
    private BankAccount findAndLockAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> BankException.accountNotFound(accountNumber));
    }

    /**
     * Executes the actual fund transfer between two accounts.
     * Validates sufficient balance and performs atomic debit/credit operations.
     * 
     * @param fromBankAccount the source account (will be debited)
     * @param toBankAccount the destination account (will be credited)
     * @param amount the transfer amount
     * @throws BankException if insufficient balance in source account
     */
    private void executeTransfer(BankAccount fromBankAccount, BankAccount toBankAccount, BigDecimal amount) {
        // verify balance
        if (fromBankAccount.getBalance().compareTo(amount) < 0) {
            throw BankException.insufficientBalance(
                    fromBankAccount.getAccountNumber(),
                    fromBankAccount.getBalance(),
                    amount
            );
        }
        fromBankAccount.withdraw(amount);
        toBankAccount.deposit(amount);
        // save account and optimistic lock will effective
        accountRepository.save(fromBankAccount);
        accountRepository.save(toBankAccount);
    }

    /**
     * Deposits money into the specified account.
     * Uses pessimistic locking for thread safety.
     * 
     * @param accountNumber the target account number
     * @param amount the amount to deposit (must be positive)
     * @return BankAccount entity with updated balance
     * @throws BankException if account is not found or amount is invalid
     */
    @Transactional
    @CacheEvict(value = {"account", "account-pages"}, key = "#accountNumber")
    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        log.info("Processing deposit: {} to account {}", amount, accountNumber);

        BankAccount bankAccount = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> BankException.accountNotFound(accountNumber));

        bankAccount.deposit(amount);
        BankAccount updatedBankAccount = accountRepository.save(bankAccount);

        log.info("Deposit completed. New balance: {}", updatedBankAccount.getBalance());
        return updatedBankAccount;
    }

    /**
     * Withdraws money from the specified account.
     * Uses pessimistic locking for thread safety and validates sufficient balance.
     * 
     * @param accountNumber the source account number
     * @param amount the amount to withdraw (must be positive and not exceed balance)
     * @return BankAccount entity with updated balance
     * @throws BankException if account is not found, insufficient funds, or amount is invalid
     */
    @Transactional
    @CacheEvict(value = {"account", "account-pages"}, key = "#accountNumber")
    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        log.info("Processing withdrawal: {} from account {}", amount, accountNumber);

        BankAccount bankAccount = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> BankException.accountNotFound(accountNumber));

        bankAccount.withdraw(amount);
        BankAccount updatedBankAccount = accountRepository.save(bankAccount);

        log.info("Withdrawal completed. New balance: {}", updatedBankAccount.getBalance());
        return updatedBankAccount;
    }
}