package com.example.finance.dto.reportdto;

import com.example.finance.enums.CategoryName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportCategoryResponse {
    private CategoryName type;
    private BigDecimal total;
    private List<CategoryData> data;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryData{
        private String category;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}

