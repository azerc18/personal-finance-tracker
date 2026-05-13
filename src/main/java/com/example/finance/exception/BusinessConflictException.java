package com.example.finance.exception;

import org.springframework.http.HttpStatus;

public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) {
        super(message);
    }

    public HttpStatus getStatus(){
        return HttpStatus.CONFLICT;
    }
}
