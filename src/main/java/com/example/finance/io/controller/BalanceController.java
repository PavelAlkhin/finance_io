package com.example.finance.io.controller;

import com.example.finance.io.controller.api.BalanceApi;
import com.example.finance.io.model.Transaction;
import com.example.finance.io.model.dto.BalanceResponse;
import com.example.finance.io.model.dto.CreateBalanceRequest;
import com.example.finance.io.model.dto.TransactionRequest;
import com.example.finance.io.service.BalanceService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class BalanceController implements BalanceApi {
    private final BalanceService service;

    @Override
    @PostMapping("/balances")
    public BalanceResponse create(@RequestBody CreateBalanceRequest req) {
        var b = service.createBalance(req.getName());
        return new BalanceResponse(b.getName(), java.math.BigDecimal.ZERO);
    }

    @Override
    @GetMapping("/balances/{name}")
    public BalanceResponse get(@PathVariable String name) {
        var balance = service.getBalance(name);
        return new BalanceResponse(name, balance);
    }

    @Override
    @PostMapping("/balances/{name}/transactions")
    public Transaction add(
            @PathVariable String name,
            @RequestBody TransactionRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return service.addTransaction(name, req.getType(), req.getAmount(), req.getCurrency(), idempotencyKey);
    }

    @Override
    @GetMapping("/balances/{name}/transactions")
    public List<Transaction> allTx(@PathVariable String name) {
        return service.getTransactions(name);
    }
}
