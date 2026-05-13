package com.example.finance.controller;

import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.budgetservice.BudgetService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @WebMvcTest(BudgetController.class)
    @Nested
    @DisplayName("Budget Controller")
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