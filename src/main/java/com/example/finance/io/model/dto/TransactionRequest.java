package com.example.finance.io.model.dto;

import com.example.finance.io.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
}
