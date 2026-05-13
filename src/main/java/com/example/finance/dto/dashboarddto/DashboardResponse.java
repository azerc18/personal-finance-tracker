package com.example.finance.dto.dashboarddto;

import com.example.finance.enums.CategoryName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal balance;
    private List<pieChartData> pieChart;
    private List<recentTransactionsData> recentTransactions;

    @Data
    @AllArgsConstructor
    @Builder
    public static class pieChartData {
        private String category;
        private BigDecimal amount;
        private CategoryName  type;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class recentTransactionsData {
        private Long id;
        private String category;
        private String icon;
        private BigDecimal amount;
        private LocalDate date;
        private CategoryName type;
        private String iconUrl;
    }
}
