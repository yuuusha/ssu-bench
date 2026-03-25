package com.diev.exception;

public class BadRequestException extends AppException {
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

}