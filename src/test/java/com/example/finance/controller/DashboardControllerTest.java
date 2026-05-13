package com.example.finance.controller;

import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.dashboardservice.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @WebMvcTest(DashboardController.class)
    @Nested
    @DisplayName("Dashboard Controller")
    class DashboardControllerTest {

        @Autowired MockMvc mockMvc;
        @MockitoBean
        DashboardService dashboardService;
        @MockitoBean
        JwtService jwtService;

        @MockitoBean
        CustomUserDetailsService customUserDetailsService;

        /** TC-DASH – Không có token → 401 */
        @Test
        @DisplayName("Không có token trả về 401")
        void getDashboard_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/dashboards"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-DASH-01 – Có token → 200, đủ các field */
        @Test
        @WithMockUser
        @DisplayName("TC-DASH-01: Có token trả về 200 với đủ income, expenses, balance, pieChart, recentTransactions")
        void getDashboard_withToken_returns200WithAllFields() throws Exception {
            DashboardResponse mockRes = DashboardResponse.builder()
                    .income(BigDecimal.valueOf(2600.0)).expenses(BigDecimal.valueOf(1400.0)).balance(BigDecimal.valueOf(1200.0))
                    .pieChart(List.of()).recentTransactions(List.of())
                    .build();
            when(dashboardService.getDashboard(any())).thenReturn(mockRes);

            mockMvc.perform(get("/api/dashboards").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.income").exists())
                    .andExpect(jsonPath("$.data.expenses").exists())
                    .andExpect(jsonPath("$.data.balance").exists())
                    .andExpect(jsonPath("$.data.pieChart").isArray())
                    .andExpect(jsonPath("$.data.recentTransactions").isArray());
        }
    }