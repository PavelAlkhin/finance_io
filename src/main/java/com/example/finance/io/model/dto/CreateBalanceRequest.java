package com.example.finance.io.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBalanceRequest {
    private String name;
}
