package com.example.finance.io.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(name = "ApiErrorDto", description = "Unified error structure")
public record ApiErrorDto(
        String title,
        String detail,
        int status,
        String uri,
        List<ApiErrorDto> errors
) {
}
