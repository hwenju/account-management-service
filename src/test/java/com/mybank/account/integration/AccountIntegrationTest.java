package com.mybank.account.integration;

import com.mybank.account.dto.request.CreateAccountRequest;
import com.mybank.account.dto.request.TransferRequest;
import com.mybank.account.dto.response.AccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndTransfer_ShouldWork() {
        // Create first account
        CreateAccountRequest account1Request = new CreateAccountRequest();
        account1Request.setAccountNumber("INT001");
        account1Request.setAccountHolderName("Integration Test 1");
        account1Request.setInitialBalance(new BigDecimal("5000.00"));

        ResponseEntity<AccountResponse> account1Response = restTemplate.postForEntity(
                "/v1/accounts", account1Request, AccountResponse.class);
        assertThat(account1Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Create second account  
        CreateAccountRequest account2Request = new CreateAccountRequest();
        account2Request.setAccountNumber("INT002");
        account2Request.setAccountHolderName("Integration Test 2");
        account2Request.setInitialBalance(new BigDecimal("3000.00"));

        ResponseEntity<AccountResponse> account2Response = restTemplate.postForEntity(
                "/v1/accounts", account2Request, AccountResponse.class);
        assertThat(account2Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Transfer between accounts
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber("INT001");
        transferRequest.setToAccountNumber("INT002");
        transferRequest.setAmount(new BigDecimal("1000.00"));

        ResponseEntity<Void> transferResponse = restTemplate.postForEntity(
                "/v1/accounts/transfer", transferRequest, Void.class);
        assertThat(transferResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify balances
        ResponseEntity<AccountResponse> afterTransfer1 = restTemplate.getForEntity(
                "/v1/accounts/INT001", AccountResponse.class);
        ResponseEntity<AccountResponse> afterTransfer2 = restTemplate.getForEntity(
                "/v1/accounts/INT002", AccountResponse.class);

        assertThat(afterTransfer1.getBody().getBalance()).isEqualTo("4000.00");
        assertThat(afterTransfer2.getBody().getBalance()).isEqualTo("4000.00");
    }
}