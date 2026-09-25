package com.example.bem.web;

import com.example.bem.aero.ConvergenceException;
import com.example.bem.validation.ApiException;
import com.example.bem.validation.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Translates exceptions into uniform {@code {code, message, status}} bodies so
 * illegal input and non-convergence always arrive as readable, machine-coded
 * error responses rather than stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message, int status, OffsetDateTime time) {
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(body(ex.getErrorCode().code(), ex.getMessage(), ex.getHttpStatus()));
    }

    @ExceptionHandler(ConvergenceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleConvergence(ConvergenceException ex) {
        return body(ErrorCode.ITERATION_NOT_CONVERGED.code(), ex.getMessage(), 422);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException ex) {
        return body(ErrorCode.INVALID_JSON.code(),
                "request body could not be parsed as JSON", 400);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOther(Exception ex) {
        return body(ErrorCode.INTERNAL_ERROR.code(),
                "unexpected server error: " + ex.getClass().getSimpleName(), 500);
    }

    private ErrorResponse body(String code, String message, int status) {
        return new ErrorResponse(code, message, status, OffsetDateTime.now());
    }
}
