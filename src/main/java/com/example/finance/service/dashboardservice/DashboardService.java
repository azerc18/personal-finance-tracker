package com.example.finance.service.dashboardservice;

import com.example.finance.dto.dashboarddto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(String userEmail);
}
