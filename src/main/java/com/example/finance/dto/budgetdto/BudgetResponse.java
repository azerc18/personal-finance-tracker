package com.example.finance.dto.budgetdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetResponse {
    private Long id;
    private CategoryData category;
    private BigDecimal amount;
    private Integer month;
    private Integer year;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryData{
        private Long id;
        private String name;
        private String icon;
        private String iconUrl;
    }
}
