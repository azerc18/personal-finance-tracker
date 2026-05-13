package com.example.finance.dto.budgetdto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAmountRequest {
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be > 0")
    private BigDecimal amount;
}
