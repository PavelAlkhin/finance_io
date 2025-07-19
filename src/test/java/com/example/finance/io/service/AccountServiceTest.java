package com.example.finance.io.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.io.model.Account;
import com.example.finance.io.model.Transaction;
import com.example.finance.io.model.TransactionType;
import com.example.finance.io.repository.AccountRepository;
import com.example.finance.io.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private final CurrencyConverter currencyConverter = new CurrencyConverter();

    private AccountBalanceService accountBalanceService;

    private final String BALANCE_NAME = "main-account";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        accountBalanceService = new AccountBalanceService(accountRepository, transactionRepository, currencyConverter);
    }

    @Test
    void createBalance_success() {
        when(accountRepository.findByName(BALANCE_NAME)).thenReturn(Optional.empty());
        Account newAccount = Account.builder().id(1L).name(BALANCE_NAME).build();
        when(accountRepository.save(any())).thenReturn(newAccount);

        Account b = accountBalanceService.createBalance(BALANCE_NAME);

        assertEquals(BALANCE_NAME, b.getName());
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void createBalance_alreadyExists() {
        when(accountRepository.findByName(BALANCE_NAME))
                .thenReturn(Optional.of(Account.builder().name(BALANCE_NAME).build()));

        assertThrows(IllegalArgumentException.class, () -> accountBalanceService.createBalance(BALANCE_NAME));
    }

    @Test
    void getBalance_throwsIfNotFound() {
        when(accountRepository.findByName("bad")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> accountBalanceService.getBalance("bad"));
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

        Transaction result = accountBalanceService.addTransaction(
                BALANCE_NAME, TransactionType.DEPOSIT, BigDecimal.TEN, "USD", idemKey);

        assertEquals(existing, result);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void addTransaction_createsNewIfNoIdempotency() {
        String idemKey = "idem-456";
        Account account = Account.builder().id(1L).name(BALANCE_NAME).build();
        when(transactionRepository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty());
        when(accountRepository.findByName(BALANCE_NAME)).thenReturn(Optional.of(account));

        Transaction toSave = Transaction.builder()
                .account(account)
                .type(TransactionType.WITHDRAW)
                .amount(BigDecimal.valueOf(5))
                .currency("EUR")
                .idempotencyKey(idemKey)
                .build();

        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction created = accountBalanceService.addTransaction(
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
        Account account = Account.builder().id(2L).name(balanceName).build();

        when(accountRepository.findByName(balanceName)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Deposit $100 USD
        Transaction depositUsd = accountBalanceService.addTransaction(
                balanceName, TransactionType.DEPOSIT, BigDecimal.valueOf(100), "USD", "key-usd");
        assertEquals(TransactionType.DEPOSIT, depositUsd.getType());
        assertEquals(BigDecimal.valueOf(100), depositUsd.getAmount());
        assertEquals("USD", depositUsd.getCurrency());

        // Deposit €50 EUR
        Transaction depositEur = accountBalanceService.addTransaction(
                balanceName, TransactionType.DEPOSIT, BigDecimal.valueOf(50), "EUR", "key-eur");
        assertEquals(TransactionType.DEPOSIT, depositEur.getType());
        assertEquals(BigDecimal.valueOf(50), depositEur.getAmount());
        assertEquals("EUR", depositEur.getCurrency());

        // Withdraw €20 EUR
        Transaction withdrawEur = accountBalanceService.addTransaction(
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
                .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal result = accountBalanceService.getBalance(balanceName);
        assertEquals(expected, result.setScale(2, RoundingMode.HALF_DOWN));
    }
}