package com.example.finance.controller;


import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.BudgetResponse;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;
import com.example.finance.service.budgetservice.BudgetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Validated
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public ApiResponse<BudgetResponse> create(@Valid @RequestBody BudgetRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails){
        BudgetResponse response = budgetService.create(request, userDetails.getUsername());

        return new ApiResponse<>(true, "Budget saved successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public  ApiResponse<List<BudgetResponse>> getAll(
            @RequestParam @Min(value = 1, message = "Month must be between 1-12")
            @Max(value = 12, message = "Month must be between 1-12") @NotNull(message = "Month is required") Integer month,
            @RequestParam @NotNull(message = "Year is required") Integer year,
            @AuthenticationPrincipal UserDetails userDetails){

        List<BudgetResponse> responses = budgetService.getAll(month, year, userDetails.getUsername());

        return new ApiResponse<>(true, "Budget list fetched successfully", responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id ,
            @AuthenticationPrincipal UserDetails userDetails){

        budgetService.delete(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ApiResponse<BudgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAmountRequest request,
            @AuthenticationPrincipal UserDetails userDetails){

        BudgetResponse response = budgetService.update(id, request, userDetails.getUsername());

        return new ApiResponse<>(true, "Budget updated successfully", response);
    }
}
