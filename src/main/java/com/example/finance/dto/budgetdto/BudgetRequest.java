package com.example.finance.dto.budgetdto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Min(value = 1)
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Min(value = 1, message = "Month must be in range 1-12")
    @Max(value = 12, message = "Month must be in range 1-12")
    @NotNull(message = "Month is required")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;
}
