package com.diev.exception;

public class ForbiddenException extends AppException {
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

}