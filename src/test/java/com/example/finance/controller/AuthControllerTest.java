package com.example.finance.controller;

import com.example.finance.dto.logindto.LoginRequest;
import com.example.finance.dto.logindto.LoginResponse;
import com.example.finance.dto.registerdto.RegisterRequest;
import com.example.finance.dto.registerdto.RegisterResponse;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.EmailVerificationException;
import com.example.finance.security.CustomUserDetailsService;
import com.example.finance.security.JwtService;
import com.example.finance.service.authservice.AuthService;
import com.example.finance.service.emailverificationservice.VerificationTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    VerificationTokenService tokenService;

    // =========================================================
    // LOGIN
    // =========================================================
    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        /** TC-AUTH-LOGIN-01 – Đăng nhập thành công → 200 + accessToken */
        @Test
        @DisplayName("TC-AUTH-LOGIN-01: Đăng nhập thành công trả về 200 và accessToken")
        void login_success_returns200AndToken() throws Exception {
            LoginRequest req = new LoginRequest("user@example.com", "abc12345");
            LoginResponse res = new LoginResponse("eyJhbGci...", System.currentTimeMillis());
            when(authService.login(any())).thenReturn(res);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.expire").isNotEmpty());
        }

        /** TC-AUTH-LOGIN-02 – Sai mật khẩu → 401 */
        @Test
        @DisplayName("TC-AUTH-LOGIN-02: Sai mật khẩu trả về 401")
        void login_wrongPassword_returns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

            LoginRequest req = new LoginRequest("user@example.com", "wrongpass1");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        /** TC-AUTH-LOGIN-03 – Email không tồn tại → 401 (cùng message với sai pass) */
        @Test
        @DisplayName("TC-AUTH-LOGIN-03: Email không tồn tại trả về 401, không lộ lý do")
        void login_emailNotFound_returns401WithGenericMessage() throws Exception {
            when(authService.login(any())).thenThrow(new UsernameNotFoundException("Invalid email or password"));

            LoginRequest req = new LoginRequest("notexist@example.com", "abc12345");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        /** TC-AUTH-LOGIN-04 – Email chưa xác thực → 403 */
        @Test
        @DisplayName("TC-AUTH-LOGIN-04: Email chưa xác thực trả về 403")
        void login_emailNotVerified_returns403() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new EmailVerificationException("Please verify your email before logging in"));

            LoginRequest req = new LoginRequest("unverified@example.com", "abc12345");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));
        }

        /** TC-AUTH-LOGIN-05 – Email sai định dạng → 422 validation error */
        @Test
        @DisplayName("TC-AUTH-LOGIN-05: Email sai định dạng trả về 422 với field error")
        void login_invalidEmailFormat_returns422() throws Exception {
            LoginRequest req = new LoginRequest("abc123", "abc12345");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid email format."));
        }

        /** TC-AUTH-LOGIN-06 – Password bị bỏ trống → 422 */
        @Test
        @DisplayName("TC-AUTH-LOGIN-06: Password bị bỏ trống trả về 422")
        void login_blankPassword_returns422() throws Exception {
            LoginRequest req = new LoginRequest("user@example.com", "");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("password"))
                    .andExpect(jsonPath("$.errors[0].message").value("Password is required."));
        }

        /** TC-AUTH-LOGIN-07 – Nhiều field invalid cùng lúc → 422, errors là array */
        @Test
        @DisplayName("TC-AUTH-LOGIN-07: Nhiều field invalid trả về 422 với danh sách lỗi")
        void login_multipleInvalidFields_returns422WithErrorList() throws Exception {
            LoginRequest req = new LoginRequest("abc123", "123");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        /** TC-AUTH-LOGIN-08 – Thiếu cả 2 field → 422 */
        @Test
        @DisplayName("TC-AUTH-LOGIN-08: Body rỗng trả về 422 với ít nhất 2 lỗi")
        void login_emptyBody_returns422() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
    }

    // =========================================================
    // REGISTER
    // =========================================================
    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        /** TC-AUTH-REG-01 – Đăng ký thành công → 201 */
        @Test
        @DisplayName("TC-AUTH-REG-01: Đăng ký thành công trả về 201 và thông tin user")
        void register_success_returns201() throws Exception {
            RegisterRequest req = new RegisterRequest("Nguyễn Văn A", "user@example.com", "abc12345");
            RegisterResponse res = new RegisterResponse(1L, "Nguyễn Văn A", "user@example.com");
            when(authService.register(any())).thenReturn(res);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(org.hamcrest.Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"));
        }

        /** TC-AUTH-REG-02 – Email đã tồn tại → 409 */
        @Test
        @DisplayName("TC-AUTH-REG-02: Email đã tồn tại trả về 409")
        void register_duplicateEmail_returns409() throws Exception {
            when(authService.register(any())).thenThrow(new BusinessConflictException("Email is already registered"));

            RegisterRequest req = new RegisterRequest("Test", "existing@example.com", "abc12345");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email is already registered"));
        }

        /** TC-AUTH-REG-03 – Email sai định dạng → 422 */
        @Test
        @DisplayName("TC-AUTH-REG-03: Email sai định dạng trả về 422")
        void register_invalidEmail_returns422() throws Exception {
            RegisterRequest req = new RegisterRequest("Test", "invalidemail", "abc12345");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid email format"));
        }

        /** TC-AUTH-REG-04 – Password quá ngắn < 8 ký tự → 422 */
        @Test
        @DisplayName("TC-AUTH-REG-04: Password < 8 ký tự trả về 422")
        void register_passwordTooShort_returns422() throws Exception {
            RegisterRequest req = new RegisterRequest("Test", "user@example.com", "abc");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("password"));
        }

        /** TC-AUTH-REG-05 – Password không có số → 422 */
        @Test
        @DisplayName("TC-AUTH-REG-05: Password không có số trả về 422")
        void register_passwordNoDigit_returns422() throws Exception {
            RegisterRequest req = new RegisterRequest("Test", "user@example.com", "abcdefgh");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("password"));
        }

        /** TC-AUTH-REG-06 – fullName bị bỏ trống → 422 */
        @Test
        @DisplayName("TC-AUTH-REG-06: fullName rỗng trả về 422")
        void register_blankFullName_returns422() throws Exception {
            RegisterRequest req = new RegisterRequest("", "user@example.com", "abc12345");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("fullName"))
                    .andExpect(jsonPath("$.errors[0].message").value("Full name is required."));
        }

        /** TC-AUTH-REG-07 – Nhiều field invalid → 422, errors có 3 phần tử */
        @Test
        @DisplayName("TC-AUTH-REG-07: 3 field invalid cùng lúc trả về 422 với 3 lỗi")
        void register_allFieldsInvalid_returns422With3Errors() throws Exception {
            RegisterRequest req = new RegisterRequest("", "invalidemail", "abc");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors.length()").value(3));
        }

        /** TC-AUTH-REG-09 – fullName null → 422 */
        @Test
        @DisplayName("TC-AUTH-REG-09: fullName null trả về 422")
        void register_nullFullName_returns422() throws Exception {
            String body = "{\"email\":\"user@example.com\",\"password\":\"abc12345\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].field").value("fullName"));
        }
    }
}