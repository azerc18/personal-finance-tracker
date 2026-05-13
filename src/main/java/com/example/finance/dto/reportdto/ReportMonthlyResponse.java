package com.example.finance.dto.reportdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportMonthlyResponse {
    private List<ChartData> chart;
    private Summary summary;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartData{
        private String monthName;
        private BigDecimal income;
        private BigDecimal expense;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary{
        private String month;
        private BigDecimal income;
        private BigDecimal expense;

    }
}
