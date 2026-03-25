package com.diev.exception;

import lombok.Getter;

@Getter
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    protected AppException(ErrorCode errorCode, String message) {
        super(resolveMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public org.springframework.http.HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        return message == null || message.isBlank()
                ? errorCode.getDefaultMessage()
                : message;
    }
}