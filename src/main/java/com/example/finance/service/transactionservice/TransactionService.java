package com.example.finance.service.transactionservice;

import com.example.finance.dto.transactiondto.TransactionRequest;
import com.example.finance.dto.transactiondto.TransactionResponse;
import com.example.finance.enums.CategoryName;


import java.time.LocalDate;
import java.util.List;


public interface TransactionService {
    TransactionResponse add(TransactionRequest request, String currentUser);
    List<TransactionResponse> getByHistory(String email, CategoryName type, LocalDate startDate, LocalDate endDate);
}
