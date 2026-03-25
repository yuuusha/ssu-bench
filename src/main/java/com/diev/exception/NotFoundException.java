package com.diev.exception;

public class NotFoundException extends AppException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

}