package com.example.finance.io.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "balance_id")
    private Balance balance;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amountUSD;

    private String currency;

    private LocalDateTime timestamp;

    @Column(unique = true)
    private String idempotencyKey;
}
