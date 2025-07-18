package com.example.finance.io.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    private String name;
    private BigDecimal balance;
}
