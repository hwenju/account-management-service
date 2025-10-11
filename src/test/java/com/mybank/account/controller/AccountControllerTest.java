package com.mybank.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.entity.BankAccount;
import com.mybank.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createAccount_ShouldReturnCreated() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC001");
        request.setAccountHolderName("John Doe");
        request.setInitialBalance(new BigDecimal("1000.00"));

        BankAccount account = new BankAccount();
        account.setId(1L);
        account.setAccountNumber("ACC001");
        account.setBalance(new BigDecimal("1000.00"));

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(account);

        // When & Then
        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    void getAccount_ShouldReturnAccount() throws Exception {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        account.setBalance(new BigDecimal("500.00"));

        when(accountService.getAccountByNumber("ACC001")).thenReturn(account);

        // When & Then
        mockMvc.perform(get("/v1/accounts/ACC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void getAllAccounts_ShouldReturnPagedResult() throws Exception {
        // Given
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC001");
        Page<BankAccount> page = new PageImpl<>(List.of(account));

        when(accountService.getAllAccounts(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value("ACC001"));
    }

    @Test
    void transferFunds_ShouldReturnOk() throws Exception {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FROM001");
        request.setToAccountNumber("TO002");
        request.setAmount(new BigDecimal("100.00"));

        // When & Then
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}