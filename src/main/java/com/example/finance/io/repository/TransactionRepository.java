package com.example.finance.io.repository;

import com.example.finance.io.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccount_NameOrderByTimestampDesc(String name);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
