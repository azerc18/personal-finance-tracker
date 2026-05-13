package com.example.finance.controller;

import com.example.finance.dto.profiledto.ProfileRequest;
import com.example.finance.dto.profiledto.ProfileResponse;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.userservice.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Nested
@DisplayName("UserProfileController – Xác thực & Validation")
public class ProfileControllerTest {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;
        @MockitoBean
        UserService userProfileService;

        @MockitoBean
        JwtService jwtService;

        @MockitoBean
        CustomUserDetailsService customUserDetailsService;

        /** TC-PROFILE-05 – Không có token → 401 */
        @Test
        @DisplayName("TC-PROFILE-05: PUT /api/user/profile không có token trả về 401")
        void updateProfile_noToken_returns401() throws Exception {
            mockMvc.perform(put("/api/user/profile")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\"}"))
                    .andExpect(status().isUnauthorized());
        }

        /** TC-PROFILE-01 – Cập nhật thành công → 200 */
        @Test
        @WithMockUser
        @DisplayName("TC-PROFILE-01: Cập nhật profile hợp lệ trả về 200 với data đúng")
        void updateProfile_valid_returns200() throws Exception {
            ProfileResponse res = new ProfileResponse(1L, "Nguyễn Văn A",
                    "user@example.com", "https://cdn.example.com/avatars/user123.png");
            when(userProfileService.updateProfile(any(), any())).thenReturn(res);

            ProfileRequest req = new ProfileRequest("Nguyễn Văn A",
                    "https://cdn.example.com/avatars/user123.png");
            mockMvc.perform(put("/api/user/profile").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                    .andExpect(jsonPath("$.data.avatar").value("https://cdn.example.com/avatars/user123.png"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"));
        }

        /** TC-PROFILE-02 – fullName rỗng → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-PROFILE-02: fullName rỗng trả về 422")
        void updateProfile_blankFullName_returns422() throws Exception {
            ProfileRequest req = new ProfileRequest("", null);
            mockMvc.perform(put("/api/user/profile").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("fullName"))
                    .andExpect(jsonPath("$.errors[0].message").value("Full name is required"));
        }

        /** TC-PROFILE-03 – avatar không phải URL hợp lệ → 422 */
        @Test
        @WithMockUser
        @DisplayName("TC-PROFILE-03: avatar không phải URL hợp lệ trả về 422")
        void updateProfile_invalidAvatarUrl_returns422() throws Exception {
            ProfileRequest req = new ProfileRequest("Test", "not-a-url");
            mockMvc.perform(put("/api/user/profile").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("avatar"))
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid avatar URL"));
        }

        /** TC-PROFILE-04 – avatar null là optional → 200 */
        @Test
        @WithMockUser
        @DisplayName("TC-PROFILE-04: avatar null vẫn cập nhật thành công")
        void updateProfile_nullAvatar_returns200() throws Exception {
            ProfileResponse res = new ProfileResponse(1L, "Test", "user@example.com", null);
            when(userProfileService.updateProfile(any(), any())).thenReturn(res);

            ProfileRequest req = new ProfileRequest("Test", null);
            mockMvc.perform(put("/api/user/profile").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
}
