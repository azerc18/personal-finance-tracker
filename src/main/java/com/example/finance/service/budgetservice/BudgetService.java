package com.example.finance.service.budgetservice;

import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.BudgetResponse;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;

import java.util.List;

public interface BudgetService {
    BudgetResponse create(BudgetRequest request, String email);
    BudgetResponse update(Long id, UpdateAmountRequest request, String email);
    List<BudgetResponse> getAll(Integer month, Integer year,String email);
    void delete(Long id, String email);


}
