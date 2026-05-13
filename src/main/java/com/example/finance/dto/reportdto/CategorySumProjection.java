package com.example.finance.dto.reportdto;

import java.math.BigDecimal;

public interface CategorySumProjection {
    String getCategoryName();
    BigDecimal getTotalAmount();
}
