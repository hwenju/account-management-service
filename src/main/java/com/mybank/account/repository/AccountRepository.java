package com.mybank.account.repository;

import com.mybank.account.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * bank account repository
 */
@Repository
public interface AccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * query by account number
     */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * query by account number with lock
     * lock because of concurrency
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BankAccount a WHERE a.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /**
     * check if account exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * page query
     */
    Page<BankAccount> findAll(Pageable pageable);

    /**
     * query by account holder name
     */
    Page<BankAccount> findByAccountHolderName(String holderName, Pageable pageable);
}