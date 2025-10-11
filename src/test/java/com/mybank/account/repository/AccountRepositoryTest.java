// BankAccountRepositoryTest.java
package com.mybank.account.repository;

import com.mybank.account.entity.BankAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByAccountNumber_WhenExists_ShouldReturnAccount() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("TEST001");
        account.setAccountHolderName("Test User");
        account.setBalance(new BigDecimal("1000.00"));
        entityManager.persistAndFlush(account);

        // When
        Optional<BankAccount> found = accountRepository.findByAccountNumber("TEST001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountHolderName()).isEqualTo("Test User");
    }

    @Test
    void existsByAccountNumber_WhenExists_ShouldReturnTrue() {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("EXIST001");
        account.setAccountHolderName("Test User");
        entityManager.persistAndFlush(account);

        // When & Then
        assertThat(accountRepository.existsByAccountNumber("EXIST001")).isTrue();
    }
}