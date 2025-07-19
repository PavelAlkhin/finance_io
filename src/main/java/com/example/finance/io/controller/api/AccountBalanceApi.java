package com.example.finance.io.controller.api;

import com.example.finance.io.model.Transaction;
import com.example.finance.io.model.dto.BalanceResponse;
import com.example.finance.io.model.dto.CreateBalanceRequest;
import com.example.finance.io.model.dto.TransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "Balance API", description = "Управление счетом и транзакциями")
public interface AccountBalanceApi {

    @Operation(summary = "Создать баланс", description = "Создаёт новый баланс с нулевым значением")
    BalanceResponse create(@RequestBody CreateBalanceRequest req);

    @Operation(summary = "Получить баланс", description = "Возвращает текущий баланс по имени")
    BalanceResponse get(@PathVariable String name);

    @Operation(
            summary = "Добавить транзакцию",
            description = "Добавляет депозит или снятие средств по балансу с поддержкой идемпотентности"
    )
    Transaction add(
            @PathVariable String name,
            @RequestBody TransactionRequest req,
            @Parameter(description = "Ключ идемпотентности")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    );

    @Operation(summary = "Список транзакций", description = "Все транзакции по балансу, новые сверху")
    List<Transaction> allTx(@PathVariable String name);
}
