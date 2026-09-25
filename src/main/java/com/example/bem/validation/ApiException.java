package com.example.bem.validation;

/**
 * Raised when a request fails input validation or an analysis cannot be
 * completed (e.g. non-convergence). Carries a stable machine code alongside a
 * human-readable message so clients can branch on {@link ErrorCode}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatus;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, 400);
    }

    public ApiException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
