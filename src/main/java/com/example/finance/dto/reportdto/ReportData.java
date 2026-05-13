package com.example.finance.dto.reportdto;

import com.example.finance.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportData {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private boolean isExcess;

    private List<Transaction> transactions;
    private List<ExpenseItems> topExpenseList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ExpenseItems{
        private String category;
        private BigDecimal amount;
        private double percentage;
    }
}
