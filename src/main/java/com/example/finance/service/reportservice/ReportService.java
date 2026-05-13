package com.example.finance.service.reportservice;

import com.example.finance.dto.reportdto.*;
import com.example.finance.enums.CategoryName;


public interface ReportService {
    ReportCategoryResponse getCategoryReport(CategoryName type, int month, int year, String email);
    ReportMonthlyResponse getMonthlyReport(Integer month, int year, String email);
    ReportPDFResponse getPdfReport(ReportPDFRequest request, String email);
    void getEmailReport(ReportEmailRequest request, String email);
}
