package com.example.finance.dto.reportdto;

import com.example.finance.enums.ReportType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportEmailRequest {
    @Min(value = 1, message = "Month must be between 1 - 12")
    @Max(value = 12, message = "Month must be between 1 - 12")
    @NotNull(message = "Month is required")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;

    @Email
    private String email;

    @NotNull(message = "This field is required")
    private Boolean includeChart;

    @NotNull(message = "This field is required")
    private Boolean includeTopExpenses;

    private ReportType reportType = ReportType.SUMMARY;
}
