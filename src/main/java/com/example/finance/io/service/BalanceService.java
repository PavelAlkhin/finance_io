package com.example.finance.io.service;


import com.example.finance.io.model.Balance;
import com.example.finance.io.model.Transaction;
import com.example.finance.io.model.TransactionType;
import com.example.finance.io.repository.BalanceRepository;
import com.example.finance.io.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final BalanceRepository balanceRepo;
    private final TransactionRepository txRepo;
    private final CurrencyConverter converter;

    public Balance createBalance(String name) {
        if (balanceRepo.findByName(name).isPresent())
            throw new IllegalArgumentException("Balance already exists");
        Balance b = new Balance();
        b.setName(name);
        return balanceRepo.save(b);
    }

    @Cacheable("balance")
    public BigDecimal getBalance(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Balance name cannot be null or blank");
        }
        balanceRepo.findByName(name).orElseThrow(() -> new NoSuchElementException("No balance found"));

        List<Transaction> txs = txRepo.findByBalance_NameOrderByTimestampDesc(name);

        return txs.stream().map(tx -> {
            BigDecimal usdAmount = tx.getAmountUSD();
            return tx.getType() == TransactionType.DEPOSIT ? usdAmount : usdAmount.negate();
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @CacheEvict(value = "balance", key = "#name")
    @Transactional
    public Transaction addTransaction(String name, TransactionType type, BigDecimal amount, String currency, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Transaction> existing = txRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        var balance = balanceRepo.findByName(name)
                .orElseThrow(() -> new NoSuchElementException("No balance found"));
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .balance(balance)
                .type(type)
                .amount(amount)
                .currency(currency)
                .amountUSD(converter.toUsd(amount, currency))
                .timestamp(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
        return txRepo.save(tx);
    }

    public List<Transaction> getTransactions(String name) {
        return txRepo.findByBalance_NameOrderByTimestampDesc(name);
    }
}
