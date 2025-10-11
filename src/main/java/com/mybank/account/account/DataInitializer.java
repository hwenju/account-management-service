package com.mybank.account.account;

import com.mybank.account.entity.BankAccount;
import com.mybank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample bank accounts...");

        // Create multiple test accounts automatically
        List<BankAccount> accounts = new ArrayList<>();
        
        // Define test data
        String[][] accountData = {
            {"ACC001", "张三", "zhangsan@example.com", "13800138001", "10000.00"},
            {"ACC002", "李四", "lisi@example.com", "13800138002", "5000.00"},
            {"ACC003", "王五", "wangwu@example.com", "13800138003", "2000.00"},
            {"ACC004", "赵六", "zhaoliu@example.com", "13800138004", "15000.00"},
            {"ACC005", "钱七", "qianqi@example.com", "13800138005", "7500.00"},
            {"ACC006", "孙八", "sunba@example.com", "13800138006", "30000.00"},
            {"ACC007", "周九", "zhoujiu@example.com", "13800138007", "12000.00"},
            {"ACC008", "吴十", "wushi@example.com", "13800138008", "8000.00"},
            {"ACC009", "郑一", "zhengyi@example.com", "13800138009", "25000.00"},
            {"ACC010", "王二", "wanger@example.com", "13800138010", "6000.00"},
            {"ACC011", "测试", "ceshi@example.com", "13800138011", "7000.00"}
        };

        // Create accounts from the data array
        for (String[] data : accountData) {
            BankAccount account = new BankAccount();
            account.setAccountNumber(data[0]);
            account.setAccountHolderName(data[1]);
            account.setEmail(data[2]);
            account.setPhone(data[3]);
            account.setBalance(new BigDecimal(data[4]));
            accounts.add(account);
        }

        // Save all accounts
        accountRepository.saveAll(accounts);
        
        log.info("Sample data initialization completed. Created {} accounts.", accounts.size());
    }
}