package com.diev.exception;

public class ConflictException extends AppException {
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}