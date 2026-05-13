package com.example.finance.controller;

import com.example.finance.dto.categorydto.CategoryRequest;
import com.example.finance.dto.categorydto.CategoryResponse;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.categoryservice.CategoryService;
import com.example.finance.service.emailverificationservice.VerificationTokenService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Test – CategoryController
 * Phạm vi: 401 khi thiếu token, 422 validation, 409 conflict.
 */
@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController – Xác thực & Phân quyền")
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean
    CategoryService categoryService;
    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    VerificationTokenService tokenService;

    // =========================================================
    // GET /api/categories
    // =========================================================
    @Nested
    @DisplayName("GET /api/categories")

    class GetCategories {

        /** TC-CAT-LIST-05 – Không có Bearer Token → 401 */
        @Test
        @DisplayName("TC-CAT-LIST-05: Không có token trả về 401")
        void getCategories_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-CAT-LIST-01 – Có token, không filter → 200, data có EXPENSE và INCOME */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-LIST-01: Có token, không filter trả về 200 với cả 2 nhóm")
        void getCategories_noFilter_returns200WithBothGroups() throws Exception {
            CategoryResponse.CategoryData expenseData = new CategoryResponse.CategoryData(
                    1L, "Food", CategoryName.EXPENSE, "🍔", "http://icon.url/food.png");

            CategoryResponse.CategoryData incomeData = new CategoryResponse.CategoryData(
                    4L, "Salary", CategoryName.INCOME, "💰", "http://icon.url/salary.png");

            CategoryResponse mockResponse = new CategoryResponse(
                    List.of(incomeData),
                    List.of(expenseData)
            );

            when(categoryService.getAll()).thenReturn(mockResponse);

            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // SỬA TẠI ĐÂY: Sử dụng chữ thường theo tên biến trong DTO
                    .andExpect(jsonPath("$.data.expense").isArray())
                    .andExpect(jsonPath("$.data.income").isArray())
                    .andExpect(jsonPath("$.data.expense[0].name").value("Food"))
                    .andExpect(jsonPath("$.data.income[0].name").value("Salary"));
        }

        /** TC-CAT-LIST-04 – Không có category khớp → 200, data rỗng */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-LIST-02: Không tìm thấy category trả về 200 với list rỗng")
        void getCategories_noMatch_returns200EmptyList() throws Exception {
            // Tạo object response rỗng đúng cấu trúc mới
            CategoryResponse emptyResponse = new CategoryResponse(List.of(), List.of());

            // Stubbing đúng kiểu dữ liệu
            when(categoryService.findByType("INCOME")).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/categories/search?type=INCOME"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.income").isEmpty())
                    .andExpect(jsonPath("$.data.expense").isEmpty());
        }

        /** TC-CAT-LIST-06 – Mỗi item phải có iconUrl */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-LIST-06: Mỗi category trả về có iconUrl không rỗng")
        void getCategories_eachItemHasIconUrl() throws Exception {
            CategoryResponse.CategoryData expenseData = new CategoryResponse.CategoryData(
                    1L, "Food", CategoryName.EXPENSE, "🍔", "http://icon.url/food.png");

            CategoryResponse mockResponse = new CategoryResponse(List.of(), List.of(expenseData));

            when(categoryService.findByType("EXPENSE")).thenReturn(mockResponse);

            mockMvc.perform(get("/api/categories/search").param("type", "EXPENSE"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // Sửa đường dẫn JSON ở đây:
                    .andExpect(jsonPath("$.data.expense[0].iconUrl").value("http://icon.url/food.png"))
                    .andExpect(jsonPath("$.data.expense[0].icon").value("🍔"));
        }
    }

    // =========================================================
    // POST /api/categories
    // =========================================================
    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {

        /** TC-CAT-CREATE-08 – Không có Bearer Token → 401 */
        @Test
        @DisplayName("TC-CAT-CREATE-08: Không có token trả về 401")
        void createCategory_noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Food\",\"type\":\"EXPENSE\"}"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-CAT-CREATE-01 – Tạo thành công (không emoji) → 201 */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-01: Tạo category thành công không emoji trả về 201")
        void createCategory_noEmoji_returns201() throws Exception {
            CategoryResponse.CategoryData mockData = new CategoryResponse.CategoryData(
                    1L, "Transportation", CategoryName.EXPENSE, "🚌", "http://icon.com");

            when(categoryService.create(any(CategoryRequest.class), eq("user")))
                    .thenReturn(mockData);

            CategoryRequest req = new CategoryRequest("Transportation", "EXPENSE", null);
            mockMvc.perform(post("/api/categories").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(org.hamcrest.Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$.data.name").value("Transportation"))
                    .andExpect(jsonPath("$.data.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.data.icon").isNotEmpty())
                    .andExpect(jsonPath("$.data.iconUrl").isNotEmpty());
        }

        /** TC-CAT-CREATE-03 – Tên đã tồn tại → 409 */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-03: Tên category đã tồn tại trả về 409")
        void createCategory_duplicateName_returns409() throws Exception {
            when(categoryService.create(any(), any()))
                    .thenThrow(new BusinessConflictException("Category name already exists"));

            CategoryRequest req = new CategoryRequest("Housing", "EXPENSE", null);
            mockMvc.perform(post("/api/categories").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Category name already exists"));
        }

        /** TC-CAT-CREATE-04 – Thiếu name → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-04: Thiếu name trả về 422 với field error")
        void createCategory_blankName_returns422() throws Exception {
            CategoryRequest req = new CategoryRequest("", "EXPENSE", null);
            mockMvc.perform(post("/api/categories").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.errors[0].message").value("Name is required"));
        }

        /** TC-CAT-CREATE-05 – Thiếu type → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-05: Thiếu type trả về 422")
        void createCategory_blankType_returns422() throws Exception {
            CategoryRequest req = new CategoryRequest("NewCat", "", null);
            mockMvc.perform(post("/api/categories").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("type"));
        }

        /** TC-CAT-CREATE-06 – Thiếu cả name và type → 422, 2 lỗi */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-06: Thiếu cả name và type trả về 422 với 2 lỗi")
        void createCategory_blankNameAndType_returns422With2Errors() throws Exception {
            CategoryRequest req = new CategoryRequest("", "", null);
            mockMvc.perform(post("/api/categories").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'type')]").exists());
        }

        /** TC-CAT-CREATE-07 – type không hợp lệ → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-CAT-CREATE-07: type không phải INCOME/EXPENSE trả về 422")
        void createCategory_invalidType_returns422() throws Exception {
            CategoryRequest req = new CategoryRequest("NewCat", "SAVINGS", null);
            mockMvc.perform(post("/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("type"));
        }
    }
}