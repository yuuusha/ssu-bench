package com.diev.exception;

public class InsufficientBalanceException extends ConflictException {
    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", "Not enough balance.");
    }
}