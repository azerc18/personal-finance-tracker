package com.example.finance.controller;

import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;
import com.example.finance.dto.categorydto.CategoryResponse;
import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.dto.transactiondto.TransactionRequest;
import com.example.finance.dto.transactiondto.TransactionResponse;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.budgetservice.BudgetService;
import com.example.finance.service.dashboardservice.DashboardService;
import com.example.finance.service.transactionservice.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Test – Transaction, Dashboard, Budget
 * Phạm vi: 401 khi thiếu token, 422 validation, 404 not found.
 */
@DisplayName("Transaction & Budget & Dashboard Controller – Xác thực & Phân quyền")
class TransactionControllerTest {

    // =========================================================
    // TRANSACTION CONTROLLER
    // =========================================================
    @WebMvcTest(com.example.finance.controller.TransactionController.class)
    @Nested
    @DisplayName("POST /api/transactions")
    class TransactionController {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;

        @MockitoBean
        TransactionService transactionService;

        @MockitoBean
        JwtService jwtService;

        @MockitoBean
        CustomUserDetailsService customUserDetailsService;

        /** TC-TXN-CREATE-11 – Không có token → 401 */
        @Test
        @DisplayName("TC-TXN-CREATE-11: Không có token trả về 401")
        void createTransaction_noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":100,\"categoryId\":3,\"date\":\"2024-04-28\"}"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-TXN-CREATE-01 – Tạo thành công → 201 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-01: Tạo transaction hợp lệ trả về 201")
        void createTransaction_valid_returns201() throws Exception {
            TransactionResponse res = TransactionResponse.builder()
                    .id(101L).amount(BigDecimal.valueOf(400.0)).note("Paid monthly rent").date(LocalDate.of(2024, 04, 28))
                    .category(new CategoryResponse.CategoryData(3L, "Housing", CategoryName.EXPENSE, "🏠", "https://cdn.example.com/housing.png"))
                    .build();
            when(transactionService.add(any(), any())).thenReturn(res);

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(400.0), "Paid monthly rent", 3L,"2024-04-28");
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(101))
                    .andExpect(jsonPath("$.data.amount").value(400.0))
                    .andExpect(jsonPath("$.data.category.id").value(3))
                    .andExpect(jsonPath("$.data.date").value("2024-04-28"));
        }

        /** TC-TXN-CREATE-03 – amount = 0 → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-03: amount = 0 trả về 422")
        void createTransaction_amountZero_returns422() throws Exception {
            TransactionRequest req = new TransactionRequest(BigDecimal.ZERO, null, 3L, "2024-04-28");
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
        }

        /** TC-TXN-CREATE-04 – amount âm → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-04: amount âm trả về 422")
        void createTransaction_negativeAmount_returns422() throws Exception {
            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(-100.0), null, 3L, "2024-4-28");
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
        }

        /** TC-TXN-CREATE-06 – date sai format → 400 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-06: date sai định dạng trả về 422")
        void createTransaction_invalidDateFormat_returns422() throws Exception {
            String body = "{\"amount\":100,\"categoryId\":3,\"date\":\"28-04-2024\"}";
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("date"));
        }

        /** TC-TXN-CREATE-07 – note > 255 ký tự → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-07: note > 255 ký tự trả về 422")
        void createTransaction_noteTooLong_returns422() throws Exception {
            String longNote = "a".repeat(256);
            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(100.0), longNote, 3L, "2024-04-28");
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("note"));
        }

        /** TC-TXN-CREATE-08 – Thiếu categoryId → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-08: Thiếu categoryId trả về 422")
        void createTransaction_missingCategoryId_returns422() throws Exception {
            String body = "{\"amount\":100,\"date\":\"2024-04-28\"}";
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("categoryId"));
        }

        /** TC-TXN-CREATE-09 – Thiếu date → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-TXN-CREATE-09: Thiếu date trả về 422")
        void createTransaction_missingDate_returns422() throws Exception {
            String body = "{\"amount\":100,\"categoryId\":3}";
            mockMvc.perform(post("/api/transactions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("date"));
        }
    }

    // =========================================================
    // DASHBOARD CONTROLLER
    // =========================================================
    @WebMvcTest(DashboardController.class)
    @Nested
    @DisplayName("GET /api/dashboard")
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

    // =========================================================
    // BUDGET CONTROLLER
    // =========================================================
    @WebMvcTest(BudgetController.class)
    @Nested
    @DisplayName("Budget endpoints – Xác thực & Validation")
    class BudgetControllerTest {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockitoBean
        BudgetService budgetService;
        @MockitoBean
        JwtService jwtService;

        @MockitoBean
        CustomUserDetailsService customUserDetailsService;

        /** TC-BUDGET-CREATE-08 – Không có token → 401 */
        @Test
        @DisplayName("TC-BUDGET-CREATE-08: POST /api/budgets không có token trả về 401")
        void createBudget_noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/budgets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categoryId\":3,\"amount\":2500,\"month\":4,\"year\":2024}"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-BUDGET-CREATE-03 – amount = 0 → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-CREATE-03: amount = 0 trả về 422")
        void createBudget_amountZero_returns422() throws Exception {
            BudgetRequest req = new BudgetRequest(3L, BigDecimal.ZERO, 4, 2024);
            mockMvc.perform(post("/api/budgets").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
        }

        /** TC-BUDGET-CREATE-04 – amount âm → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-CREATE-04: amount âm trả về 422")
        void createBudget_negativeAmount_returns422() throws Exception {
            BudgetRequest req = new BudgetRequest(3L, BigDecimal.valueOf(-500.00), 4, 2024);
            mockMvc.perform(post("/api/budgets").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
        }

        /** TC-BUDGET-CREATE-05 – month = 0 → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-CREATE-05: month = 0 trả về 422")
        void createBudget_monthZero_returns422() throws Exception {
            BudgetRequest req = new BudgetRequest(3L, BigDecimal.valueOf(2500.0), 0, 2024);
            mockMvc.perform(post("/api/budgets").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("month"));
        }

        /** TC-BUDGET-CREATE-06 – month = 13 → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-CREATE-06: month = 13 trả về 422")
        void createBudget_monthThirteen_returns422() throws Exception {
            BudgetRequest req = new BudgetRequest(3L, BigDecimal.valueOf(2500.0), 13, 2024);
            mockMvc.perform(post("/api/budgets").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("month"));
        }

        /** TC-BUDGET-DELETE-04 – DELETE không có token → 401 */
        @Test
        @DisplayName("TC-BUDGET-DELETE-04: DELETE không có token trả về 401")
        void deleteBudget_noToken_returns401() throws Exception {
            mockMvc.perform(delete("/api/budgets/15").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-BUDGET-DELETE-02 – ID không tồn tại → 404 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-DELETE-02: ID không tồn tại trả về 404")
        void deleteBudget_notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Budget not found"))
                    .when(budgetService).delete(eq(9999L), anyString());

            mockMvc.perform(delete("/api/budgets/9999").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Budget not found"));
        }

        /** TC-BUDGET-DELETE-01 – Xoá thành công → 204 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-DELETE-01: Xoá thành công trả về 204 không có body")
        void deleteBudget_success_returns204() throws Exception {
            mockMvc.perform(delete("/api/budgets/15").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        /** TC-BUDGET-UPDATE-05 – PUT không có token → 401 */
        @Test
        @DisplayName("TC-BUDGET-UPDATE-05: PUT không có token trả về 401")
        void updateBudget_noToken_returns401() throws Exception {
            mockMvc.perform(patch("/api/budgets/15")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":3000}"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-BUDGET-UPDATE-02 – amount = 0 → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-UPDATE-02: amount = 0 trả về 422")
        void updateBudget_amountZero_returns422() throws Exception {
            UpdateAmountRequest req = new UpdateAmountRequest(BigDecimal.ZERO);
            mockMvc.perform(patch("/api/budgets/15").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
        }

        /** TC-BUDGET-UPDATE-03 – ID không tồn tại → 404 */
        @Test
        @WithMockUser
        @DisplayName("TC-BUDGET-UPDATE-03: ID không tồn tại trả về 404")
        void updateBudget_notFound_returns404() throws Exception {
            when(budgetService.update(eq(9999L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Budget not found"));

            UpdateAmountRequest req = new UpdateAmountRequest(BigDecimal.valueOf(3000.00));
            mockMvc.perform(patch("/api/budgets/9999").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Budget not found"));
        }
    }
}