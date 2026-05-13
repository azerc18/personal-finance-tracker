package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.service.dashboardservice.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<DashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        DashboardResponse response = dashboardService.getDashboard(email);
        return new ApiResponse<DashboardResponse>(true, "Dashboard data fetched successfully", response);
    }
}
