package com.example.finance.io.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyConverter {
    private static final Map<String, BigDecimal> RATES = Map.of(
            "USD", BigDecimal.valueOf(1.0),
            "EUR", BigDecimal.valueOf(1.1),
            "BYN", BigDecimal.valueOf(0.31),
            "RUB", BigDecimal.valueOf(0.012)
    );

    public BigDecimal toUsd(BigDecimal amount, String currency) {
        BigDecimal rate = RATES.get(currency);
        if (rate == null)
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_DOWN);
    }
}
