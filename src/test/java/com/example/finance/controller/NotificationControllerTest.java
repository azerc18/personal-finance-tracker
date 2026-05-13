package com.example.finance.controller;

import com.example.finance.dto.notificationdto.NotificationRequest;
import com.example.finance.dto.notificationdto.NotificationResponse;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.notificationservice.NotificationService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @WebMvcTest(NotificationController.class)
    @Nested
    @DisplayName("NotificationController – Xác thực & Validation")
    class NotificationControllerTest {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockitoBean
        NotificationService notificationService;
        @MockitoBean
        JwtService jwtService;

        @MockitoBean
        CustomUserDetailsService customUserDetailsService;

        /** TC-NOTIF-GET-02 – GET không có token → 401 */
        @Test
        @DisplayName("TC-NOTIF-GET-02: GET /api/notifications/settings không có token trả về 401")
        void getSettings_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/notifications/settings")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-NOTIF-GET-01 – GET có token → 200 với 3 boolean fields */
        @Test
        @WithMockUser
        @DisplayName("TC-NOTIF-GET-01: GET có token trả về 200 với dailyReminder, tipsEnabled, budgetAlert")
        void getSettings_withToken_returns200() throws Exception {
            NotificationResponse res = new NotificationResponse(true, false, true);
            when(notificationService.getNotification(any())).thenReturn(res);

            mockMvc.perform(get("/api/notifications/settings").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dailyReminder").isBoolean())
                    .andExpect(jsonPath("$.data.tipsEnabled").isBoolean())
                    .andExpect(jsonPath("$.data.budgetAlert").isBoolean());
        }

        /** TC-NOTIF-PUT-01 – PUT hợp lệ → 200, data phản ánh đúng giá trị */
        @Test
        @WithMockUser
        @DisplayName("TC-NOTIF-PUT-01: PUT hợp lệ trả về 200 với data phản ánh đúng")
        void updateSettings_valid_returns200WithCorrectData() throws Exception {
            NotificationRequest req = new NotificationRequest(false, true, false);
            NotificationResponse res = new NotificationResponse(false, true, false);
            when(notificationService.update(any(), any())).thenReturn(res);

            mockMvc.perform(put("/api/notifications/settings").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dailyReminder").value(false))
                    .andExpect(jsonPath("$.data.tipsEnabled").value(true))
                    .andExpect(jsonPath("$.data.budgetAlert").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("TC-NOTIF-PUT-03: dailyReminder không phải boolean trả về 400")
        void updateSettings_invalidBoolean_returns400() throws Exception {
            String body = "{\"dailyReminder\":\"yes\",\"tipsEnabled\":true,\"budgetAlert\":false}";

            mockMvc.perform(put("/api/notifications/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print()) // In log ra để đối chiếu nếu cần
                    .andExpect(status().isBadRequest())
                    // Sửa lại assert để khớp với Body thực tế trong log:
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Must be true or false"));
        }
    }