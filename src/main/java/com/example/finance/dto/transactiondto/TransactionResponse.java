package com.example.finance.dto.transactiondto;

import com.example.finance.dto.categorydto.CategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String note;
    private CategoryResponse.CategoryData category;
    private LocalDate date;

}
