package com.mybank.account.service;

import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.dto.request.UpdateAccountRequest;
import com.mybank.account.entity.BankAccount;
import com.mybank.account.exception.BankException;
import com.mybank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_WithValidRequest_ShouldSuccess() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC001");
        request.setAccountHolderName("John Doe");
        request.setInitialBalance(new BigDecimal("1000.00"));

        when(accountRepository.existsByAccountNumber("ACC001")).thenReturn(false);
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        BankAccount result = accountService.createAccount(request);

        // Then
        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
        assertThat(result.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        verify(accountRepository).save(any(BankAccount.class));
    }

    @Test
    void createAccount_WithDuplicateAccount_ShouldThrowException() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC001");

        when(accountRepository.existsByAccountNumber("ACC001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(BankException.class)
                .hasMessageContaining("ACC001");
    }

    @Test
    void getAccount_WhenExists_ShouldReturnAccount() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));

        // When
        BankAccount result = accountService.getAccountByNumber("ACC001");

        // Then
        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    void getAccount_WhenNotExists_ShouldThrowException() {
        // Given
        when(accountRepository.findByAccountNumber("NOTEXIST")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByNumber("NOTEXIST"))
                .isInstanceOf(BankException.class);
    }

    @Test
    void getAllAccounts_ShouldReturnPagedResult() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        Page<BankAccount> page = new PageImpl<>(List.of(account));
        when(accountRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        Page<BankAccount> result = accountService.getAllAccounts(PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    void updateAccount_WithValidRequest_ShouldUpdate() {
        // Given
        BankAccount existingAccount = new BankAccount();
        existingAccount.setAccountNumber("ACC001");
        existingAccount.setAccountHolderName("Old Name");

        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(BankAccount.class))).thenReturn(existingAccount);

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAccountHolderName("New Name");

        // When
        BankAccount result = accountService.updateAccount("ACC001", request);

        // Then
        assertThat(result.getAccountHolderName()).isEqualTo("New Name");
    }

    @Test
    void deleteAccount_WithZeroBalance_ShouldSuccess() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        account.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(account));

        // When
        accountService.deleteAccount("ACC001");

        // Then
        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_WithNonZeroBalance_ShouldThrowException() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        account.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(account));

        // When & Then

        assertThatThrownBy(() -> accountService.deleteAccount("ACC001"))
                .isInstanceOf(BankException.class)
                .hasMessageContaining("balance");
        
    }

    @Test
    void transferFunds_WithSufficientBalance_ShouldSuccess() {
        // Given
        BankAccount fromAccount = new BankAccount();
        fromAccount.setAccountNumber("FROM001");
        fromAccount.setBalance(new BigDecimal("1000.00"));

        BankAccount toAccount = new BankAccount();
        toAccount.setAccountNumber("TO002");
        toAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByAccountNumberWithLock("FROM001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberWithLock("TO002")).thenReturn(Optional.of(toAccount));

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FROM001");
        request.setToAccountNumber("TO002");
        request.setAmount(new BigDecimal("200.00"));

        // When
        accountService.transferFunds(request);

        // Then
        assertThat(fromAccount.getBalance()).isEqualTo("800.00");
        assertThat(toAccount.getBalance()).isEqualTo("700.00");
        verify(accountRepository, times(2)).save(any(BankAccount.class));
    }

    @Test
    void transferFunds_WithInsufficientBalance_ShouldThrowException() {
        // Given
        BankAccount fromAccount = new BankAccount();
        fromAccount.setAccountNumber("FROM001");
        fromAccount.setBalance(new BigDecimal("100.00"));

        BankAccount toAccount = new BankAccount();
        toAccount.setAccountNumber("TO002");
        toAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByAccountNumberWithLock("FROM001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberWithLock("TO002")).thenReturn(Optional.of(toAccount));

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FROM001");
        request.setToAccountNumber("TO002");
        request.setAmount(new BigDecimal("200.00"));

        // When & Then

        assertThatThrownBy(() -> accountService.transferFunds(request))
                .isInstanceOf(BankException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void transferFunds_ToSameAccount_ShouldThrowException() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("SAME001");
        request.setToAccountNumber("SAME001");
        request.setAmount(new BigDecimal("100.00"));

        // When & Then
        assertThatThrownBy(() -> accountService.transferFunds(request))
                .isInstanceOf(BankException.class)
                .hasMessageContaining("same account");
    }
}