package com.example.finance.dto.transactiondto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be > 0")
    private BigDecimal amount;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Date is required")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Date must be in format yyyy-MM-dd"
    )
    private String date;
}
