package com.mybank.account;

import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.entity.BankAccount;
import com.mybank.account.repository.AccountRepository;
import com.mybank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountConcurrentTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        // Clear all accounts before test
        accountRepository.deleteAll();

        BankAccount accountA = new BankAccount();
        accountA.setAccountNumber("CONC001");
        accountA.setAccountHolderName("Concurrent A");
        accountA.setBalance(new BigDecimal("10000.00"));
        accountRepository.save(accountA);

        BankAccount accountB = new BankAccount();
        accountB.setAccountNumber("CONC002");
        accountB.setAccountHolderName("Concurrent B");
        accountB.setBalance(new BigDecimal("10000.00"));
        accountRepository.save(accountB);
    }

    @Test
    void concurrentTransfers_ShouldMaintainConsistency() throws InterruptedException {
        // Given
        int threadCount = 10;
        int transfersPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < transfersPerThread; j++) {
                    TransferRequest request = new TransferRequest();
                    request.setFromAccountNumber("CONC001");
                    request.setToAccountNumber("CONC002");
                    request.setAmount(new BigDecimal("1.00"));

                    try {
                        accountService.transferFunds(request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Some failures are acceptable in concurrent testing
                    }
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        BankAccount finalA = accountRepository.findByAccountNumber("CONC001").get();
        BankAccount finalB = accountRepository.findByAccountNumber("CONC002").get();

        BigDecimal totalBalance = finalA.getBalance().add(finalB.getBalance());
        assertThat(totalBalance).isEqualTo(new BigDecimal("20000.00")); // Total balance should remain unchanged

        System.out.printf("Concurrent test: Successful transfers %d times, Final balance A: %s, B: %s%n",
                successCount.get(), finalA.getBalance(), finalB.getBalance());
    }
    
    @Test
    void concurrentDepositAndWithdrawal_ShouldMaintainConsistency() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger depositSuccessCount = new AtomicInteger(0);
        AtomicInteger withdrawalSuccessCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        // Alternate between deposit and withdrawal
                        if (j % 2 == 0) {
                            accountService.deposit("CONC001", new BigDecimal("5.00"));
                            depositSuccessCount.incrementAndGet();
                        } else {
                            accountService.withdraw("CONC001", new BigDecimal("5.00"));
                            withdrawalSuccessCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Some failures are acceptable in concurrent testing
                    }
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        BankAccount finalA = accountRepository.findByAccountNumber("CONC001").get();
        
        // Net change should be zero as we deposited and withdrew same amounts same number of times
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        
        System.out.printf("Concurrent deposit/withdrawal test: Successful deposits %d times, withdrawals %d times, Final balance A: %s%n",
                depositSuccessCount.get(), withdrawalSuccessCount.get(), finalA.getBalance());
    }
}