package com.diev.exception;

public class InsufficientBalanceException extends ConflictException {
    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE);
    }

}