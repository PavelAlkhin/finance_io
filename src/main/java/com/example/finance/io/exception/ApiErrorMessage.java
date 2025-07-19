package com.example.finance.io.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiErrorMessage {

    UNEXPECTED_INTERNAL_ERROR("Unexpected internal error");

    private final String message;
}
