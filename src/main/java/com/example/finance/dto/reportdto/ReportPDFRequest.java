package com.example.finance.dto.reportdto;

import com.example.finance.enums.ReportType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportPDFRequest {
    @Min(value = 1, message = "Month must be between 1 - 12")
    @Max(value = 12, message = "Month must be between 1 - 12")
    @NotNull(message = "Month is required")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "This field is required")
    private Boolean includeChart;

    @NotNull(message = "This field is required")
    private Boolean includeTopExpenses;

    private ReportType reportType = ReportType.SUMMARY;
}
