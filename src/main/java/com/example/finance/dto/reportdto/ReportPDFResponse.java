package com.example.finance.dto.reportdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportPDFResponse {
    private String downloadUrl;
}
