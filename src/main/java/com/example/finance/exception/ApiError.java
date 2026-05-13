package com.example.finance.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ApiError {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<ErrorItem> errors;
}