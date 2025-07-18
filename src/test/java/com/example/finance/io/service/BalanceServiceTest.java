package com.example.finance.io.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.io.model.Balance;
import com.example.finance.io.model.Transaction;
import com.example.finance.io.model.TransactionType;
import com.example.finance.io.repository.BalanceRepository;
import com.example.finance.io.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private final CurrencyConverter currencyConverter = new CurrencyConverter();

    private BalanceService balanceService;

    private final String BALANCE_NAME = "main-account";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        balanceService = new BalanceService(balanceRepository, transactionRepository, currencyConverter);
    }

    @Test
    void createBalance_success() {
        when(balanceRepository.findByName(BALANCE_NAME)).thenReturn(Optional.empty());
        Balance newBalance = Balance.builder().id(1L).name(BALANCE_NAME).build();
        when(balanceRepository.save(any())).thenReturn(newBalance);

        Balance b = balanceService.createBalance(BALANCE_NAME);

        assertEquals(BALANCE_NAME, b.getName());
        verify(balanceRepository, times(1)).save(any());
    }

    @Test
    void createBalance_alreadyExists() {
        when(balanceRepository.findByName(BALANCE_NAME))
                .thenReturn(Optional.of(Balance.builder().name(BALANCE_NAME).build()));

        assertThrows(IllegalArgumentException.class, () -> balanceService.createBalance(BALANCE_NAME));
    }

    @Test
    void getBalance_throwsIfNotFound() {
        when(balanceRepository.findByName("bad")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> balanceService.getBalance("bad"));
    }

    @Test
    void addTransaction_idempotencyReturnsExisting() {
        String idemKey = "idem-123";
        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idemKey)
                .amount(BigDecimal.TEN)
                .type(TransactionType.DEPOSIT)
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdempotencyKey(idemKey)).thenReturn(Optional.of(existing));

        Transaction result = balanceService.addTransaction(
                BALANCE_NAME, TransactionType.DEPOSIT, BigDecimal.TEN, "USD", idemKey);

        assertEquals(existing, result);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void addTransaction_createsNewIfNoIdempotency() {
        String idemKey = "idem-456";
        Balance balance = Balance.builder().id(1L).name(BALANCE_NAME).build();
        when(transactionRepository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty());
        when(balanceRepository.findByName(BALANCE_NAME)).thenReturn(Optional.of(balance));

        Transaction toSave = Transaction.builder()
                .balance(balance)
                .type(TransactionType.WITHDRAW)
                .amount(BigDecimal.valueOf(5))
                .currency("EUR")
                .idempotencyKey(idemKey)
                .build();

        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction created = balanceService.addTransaction(
                BALANCE_NAME, TransactionType.WITHDRAW, BigDecimal.valueOf(5), "EUR", idemKey);

        assertEquals(TransactionType.WITHDRAW, created.getType());
        assertEquals(idemKey, created.getIdempotencyKey());
        assertEquals("EUR", created.getCurrency());
        assertEquals(BigDecimal.valueOf(5), created.getAmount());
        assertNotNull(created.getId());
    }

    @Test
    void addTransaction_depositAndWithdrawDifferentCurrencies_updatesBalanceCorrectly() {
        String balanceName = "multi-currency";
        Balance balance = Balance.builder().id(2L).name(balanceName).build();

        when(balanceRepository.findByName(balanceName)).thenReturn(Optional.of(balance));
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Deposit $100 USD
        Transaction depositUsd = balanceService.addTransaction(
                balanceName, TransactionType.DEPOSIT, BigDecimal.valueOf(100), "USD", "key-usd");
        assertEquals(TransactionType.DEPOSIT, depositUsd.getType());
        assertEquals(BigDecimal.valueOf(100), depositUsd.getAmount());
        assertEquals("USD", depositUsd.getCurrency());

        // Deposit €50 EUR
        Transaction depositEur = balanceService.addTransaction(
                balanceName, TransactionType.DEPOSIT, BigDecimal.valueOf(50), "EUR", "key-eur");
        assertEquals(TransactionType.DEPOSIT, depositEur.getType());
        assertEquals(BigDecimal.valueOf(50), depositEur.getAmount());
        assertEquals("EUR", depositEur.getCurrency());

        // Withdraw €20 EUR
        Transaction withdrawEur = balanceService.addTransaction(
                balanceName, TransactionType.WITHDRAW, BigDecimal.valueOf(20), "EUR", "key-eur-w");
        assertEquals(TransactionType.WITHDRAW, withdrawEur.getType());
        assertEquals(BigDecimal.valueOf(20), withdrawEur.getAmount());
        assertEquals("EUR", withdrawEur.getCurrency());

        // Mock transaction list for balance calculation
        List<Transaction> txs = List.of(depositUsd, depositEur, withdrawEur);
        when(transactionRepository.findByBalance_NameOrderByTimestampDesc(balanceName)).thenReturn(txs);

        BigDecimal expected = depositUsd.getAmountUSD()
                .add(depositEur.getAmountUSD())
                .subtract(withdrawEur.getAmountUSD())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal result = balanceService.getBalance(balanceName);
        assertEquals(expected, result.setScale(2, RoundingMode.HALF_UP));
    }
}