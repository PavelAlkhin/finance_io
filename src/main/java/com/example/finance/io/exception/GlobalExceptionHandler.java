package com.example.finance.io.exception;

import com.example.finance.io.model.dto.ApiErrorDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static com.example.finance.io.exception.ApiErrorMessage.UNEXPECTED_INTERNAL_ERROR;


@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleForbiddenException(IllegalArgumentException e, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.FORBIDDEN;

        ApiErrorDto apiError = buildApiErrorDto("Forbidden", e.getMessage(), request, httpStatus);

        return logAndRespond(httpStatus, apiError);
    }

    @ExceptionHandler(ExternalException.class)
    public ResponseEntity<ApiErrorDto> handleExternalException(ExternalException e, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_GATEWAY;

        ApiErrorDto apiError = buildApiErrorDto("Bad Gateway", "External service error", request, httpStatus);

        return logAndRespond(httpStatus, apiError);
    }

    @ExceptionHandler({NoSuchElementException.class, })
    public ResponseEntity<ApiErrorDto> handleHttpMessageNotReadableException(NoSuchElementException e, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ApiErrorDto apiError = buildApiErrorDto("Invalid Request Body", e.getMessage(), request, httpStatus);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneralException(Throwable e, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorDto apiError = buildApiErrorDto("Unexpected exception", UNEXPECTED_INTERNAL_ERROR.getMessage(), request, httpStatus);

        return logAndRespond(httpStatus, apiError, e);
    }

    private static ApiErrorDto buildApiErrorDto(String title, String errorDetail, HttpServletRequest request, HttpStatus status) {
        return ApiErrorDto.builder()
                .title(title)
                .detail(errorDetail)
                .status(status.value())
                .uri(request.getRequestURI())
                .build();
    }

    private ResponseEntity<ApiErrorDto> logAndRespond(HttpStatus status, ApiErrorDto apiError) {
        Object errorView;
        try {
            errorView = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiError);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize apiError to json");
            errorView = apiError;
        }

        log.error("{}\n{}", apiError.title(), errorView);
        return new ResponseEntity<>(apiError, status);
    }

    private ResponseEntity<ApiErrorDto> logAndRespond(HttpStatus status, ApiErrorDto apiError, Throwable e) {
        log.error("Exception stacktrace", e);
        return logAndRespond(status, apiError);
    }
}
