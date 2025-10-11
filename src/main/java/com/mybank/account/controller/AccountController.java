package com.mybank.account.controller;

import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.dto.request.UpdateAccountRequest;
import com.mybank.account.dto.response.AccountResponse;
import com.mybank.account.entity.BankAccount;
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
 * bank account RESTFul controller
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AccountController {

    private final AccountService accountService;
    /**
     * create bank account
     * API: POST /v1/accounts
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Received request to create account for: {}", request.getAccountHolderName());
        BankAccount bankAccount = accountService.createAccount(request);
        return toResponse(bankAccount);
    }

    /**
     * get all accounts by page
     *
     * API: GET /v1/accounts?page=0&size=20&sort=createdAt,desc
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
     * query account detail by number
     * API: GET /v1/accounts/{accountNumber}
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
     * update bank account
     * API: PUT /v1/accounts/{accountNumber}
     */
    @PutMapping("/{accountNumber}")
    public AccountResponse updateAccount(@PathVariable String accountNumber,
                                         @Valid @RequestBody UpdateAccountRequest request) {
        log.info("Received request to update account accountNumber: {}", accountNumber);
        BankAccount bankAccount = accountService.updateAccount(accountNumber, request);
        return toResponse(bankAccount);
    }

    /**
     * delete account
     * API: DELETE /v1/accounts/{accountNumber}
     */
    @DeleteMapping("/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable String accountNumber) {
        log.info("Received request to delete account accountNumber: {}", accountNumber);
        accountService.deleteAccount(accountNumber);
    }

    /**
     * transfer funds
     * API: POST /v1/accounts/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferFunds(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request: {} from {} to {}",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        accountService.transferFunds(request);
        return ResponseEntity.ok().build();
    }

    /**
     * account deposit
     * API: POST /v1/accounts/{accountNumber}/deposit
     */
    @PostMapping("/{accountNumber}/deposit")
    public AccountResponse deposit(@PathVariable String accountNumber,
                                   @RequestParam BigDecimal amount) {
        log.info("Received deposit request: {} to account {}", amount, accountNumber);
        BankAccount bankAccount = accountService.deposit(accountNumber, amount);
        return toResponse(bankAccount);
    }

    /**
     * account withdraw
     * API: POST /v1/accounts/{accountNumber}/withdraw
     */
    @PostMapping("/{accountNumber}/withdraw")
    public AccountResponse withdraw(@PathVariable String accountNumber,
                                    @RequestParam BigDecimal amount) {
        log.info("Received withdrawal request: {} from account {}", amount, accountNumber);
        BankAccount bankAccount = accountService.withdraw(accountNumber, amount);
        return toResponse(bankAccount);
    }

    /**
     * health check endpoint
     * API: GET /v1/accounts/health
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