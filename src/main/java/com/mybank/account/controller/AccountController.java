package com.mybank.account.controller;

import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.dto.request.UpdateAccountRequest;
import com.mybank.account.dto.response.AccountResponse;
import com.mybank.account.entity.BankAccount;
import com.mybank.account.exception.BankException;
import com.mybank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * RESTful controller for bank account management operations.
 * Provides endpoints for CRUD operations, fund transfers, and account transactions.
 * 
 * @author Bank Account Service
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AccountController {

    private final AccountService accountService;
    /**
     * Creates a new bank account with the provided account details.
     * Validates account number uniqueness and initializes account with specified balance.
     * 
     * @param request the account creation request containing holder details and initial balance
     * @return AccountResponse containing the created account information
     * @throws BankException if account number already exists or validation fails
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Received request to create account for: {}", request.getAccountHolderName());
        BankAccount bankAccount = accountService.createAccount(request);
        return toResponse(bankAccount);
    }

    /**
     * Retrieves all bank accounts with pagination support.
     * Supports sorting by various fields and customizable page size.
     * 
     * @param page the page number (0-based)
     * @param size the number of accounts per page
     * @param sort the field to sort by (default: createdAt)
     * @param direction the sort direction - asc or desc (default: desc)
     * @return Page<AccountResponse> containing paginated account list
     */
    @GetMapping
    public Page<AccountResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        log.debug("Fetching accounts - page: {}, size: {}, sort: {}", page, size, sort);

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<BankAccount> accounts = accountService.getAllAccounts(pageable);
        return accounts.map(this::toResponse);
    }

    /**
     * Retrieves detailed information for a specific account by account number.
     * 
     * @param accountNumber the unique account number to query
     * @return AccountResponse containing complete account details
     * @throws BankException if account is not found
     */
    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        log.debug("Fetching account details for: {}", accountNumber);
        BankAccount bankAccount = accountService.getAccountByNumber(accountNumber);
        return toResponse(bankAccount);
    }

    /**
     * query account detail by id
     * API: GET /v1/accounts/id/{id}
     */
//    @GetMapping("/id/{id}")
//    public AccountResponse getAccountById(@PathVariable Long id) {
//        log.debug("Fetching account details for ID: {}", id);
//        BankAccount bankAccount = accountService.getAccountById(id);
//        return toResponse(bankAccount);
//    }

    /**
     * Updates account details for an existing account.
     * Only non-null fields in the request will be updated.
     * 
     * @param accountNumber the account number to update
     * @param request the update request containing new account details
     * @return AccountResponse containing the updated account information
     * @throws BankException if account is not found or validation fails
     */
    @PutMapping("/{accountNumber}")
    public AccountResponse updateAccount(@PathVariable String accountNumber,
                                         @Valid @RequestBody UpdateAccountRequest request) {
        log.info("Received request to update account accountNumber: {}", accountNumber);
        BankAccount bankAccount = accountService.updateAccount(accountNumber, request);
        return toResponse(bankAccount);
    }

    /**
     * Deletes an existing bank account.
     * Account can only be deleted if the balance is zero.
     * 
     * @param accountNumber the account number to delete
     * @throws BankException if account is not found or has non-zero balance
     */
    @DeleteMapping("/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable String accountNumber) {
        log.info("Received request to delete account accountNumber: {}", accountNumber);
        accountService.deleteAccount(accountNumber);
    }

    /**
     * Transfers funds between two bank accounts.
     * Ensures thread safety with pessimistic locking and deadlock prevention.
     * 
     * @param request the transfer request containing source, destination, and amount
     * @throws BankException if accounts don't exist, insufficient funds, or same account transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferFunds(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request: {} from {} to {}",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        accountService.transferFunds(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deposits money into the specified account.
     * 
     * @param accountNumber the target account number
     * @param amount the amount to deposit (must be positive)
     * @return AccountResponse containing updated account details
     * @throws BankException if account is not found or amount is invalid
     */
    @PostMapping("/{accountNumber}/deposit")
    public AccountResponse deposit(@PathVariable String accountNumber,
                                   @RequestParam BigDecimal amount) {
        log.info("Received deposit request: {} to account {}", amount, accountNumber);
        BankAccount bankAccount = accountService.deposit(accountNumber, amount);
        return toResponse(bankAccount);
    }

    /**
     * Withdraws money from the specified account.
     * 
     * @param accountNumber the source account number
     * @param amount the amount to withdraw (must be positive and not exceed balance)
     * @return AccountResponse containing updated account details
     * @throws BankException if account is not found, insufficient funds, or amount is invalid
     */
    @PostMapping("/{accountNumber}/withdraw")
    public AccountResponse withdraw(@PathVariable String accountNumber,
                                    @RequestParam BigDecimal amount) {
        log.info("Received withdrawal request: {} from account {}", amount, accountNumber);
        BankAccount bankAccount = accountService.withdraw(accountNumber, amount);
        return toResponse(bankAccount);
    }

    /**
     * Health check endpoint for service monitoring.
     * Used by load balancers and container orchestrators to verify service availability.
     * 
     * @return ResponseEntity with service status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Bank Account Service is running");
    }

    private AccountResponse toResponse(BankAccount bankAccount) {
        AccountResponse response = new AccountResponse();
        response.setId(bankAccount.getId());
        response.setAccountNumber(bankAccount.getAccountNumber());
        response.setAccountHolderName(bankAccount.getAccountHolderName());
        response.setEmail(bankAccount.getEmail());
        response.setPhone(bankAccount.getPhone());
        response.setBalance(bankAccount.getBalance());
        response.setCreatedAt(bankAccount.getCreatedAt());
        response.setUpdatedAt(bankAccount.getUpdatedAt());
        return response;
    }
}