package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.transactiondto.TransactionRequest;
import com.example.finance.dto.transactiondto.TransactionResponse;
import com.example.finance.enums.CategoryName;
import com.example.finance.service.transactionservice.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public ApiResponse<TransactionResponse> create (@Valid @RequestBody TransactionRequest request,
                                                    @AuthenticationPrincipal UserDetails userDetails ){
        String userEmail = userDetails.getUsername();
        TransactionResponse response = transactionService.add(request, userEmail);
        return new ApiResponse<>(true, "Transaction added successfully", response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<List<TransactionResponse>> getByHistory(@RequestParam LocalDate startDate,
                                                               @RequestParam LocalDate endDate,
                                                               @RequestParam(required = false) CategoryName type,
                                                               @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();

        return new ApiResponse<>(true, "Transaction history fetched successfully", transactionService.getByHistory(email, type, startDate, endDate));
    }
}
