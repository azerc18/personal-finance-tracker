package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.reportdto.*;
import com.example.finance.enums.CategoryName;
import com.example.finance.service.reportservice.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/category")
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<ReportCategoryResponse> getCategoryReport(
            @RequestParam(required = false) CategoryName type,
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        ReportCategoryResponse response = reportService.getCategoryReport(type, month, year, email);
        return new ApiResponse<>(true, "Category breakdown fetched successfully", response);
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<ReportMonthlyResponse> getMonthlyReport(
            @RequestParam(required = false) Integer month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        ReportMonthlyResponse response = reportService.getMonthlyReport(month, year, email);
        return new ApiResponse<>(true, "Monthly financial report fetched successfully", response);
    }

    @PostMapping("/export/pdf")
    @PreAuthorize("hasAuthority('CREATE')")
    public ApiResponse<ReportPDFResponse> exportPdfReport(@Valid @RequestBody ReportPDFRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        ReportPDFResponse response = reportService.getPdfReport(request, userDetails.getUsername());
        System.out.println("Da toi");
            return new ApiResponse<>(true, "Success", reportService.getPdfReport(request, userDetails.getUsername()));
    }

    @PostMapping("export/email")
    @PreAuthorize("hasAuthority('CREATE')")
    public ApiResponse<Void> exportEmailReport(@Valid @RequestBody ReportEmailRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails){

        reportService.getEmailReport(request, userDetails.getUsername());
        return new ApiResponse<>(true,"Report sent successfully to " + request.getEmail());
    }
}
