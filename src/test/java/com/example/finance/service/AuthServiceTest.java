package com.example.finance.service;

import com.example.finance.dto.logindto.LoginRequest;
import com.example.finance.dto.logindto.LoginResponse;
import com.example.finance.dto.registerdto.RegisterRequest;
import com.example.finance.dto.registerdto.RegisterResponse;
import com.example.finance.entity.Authority;
import com.example.finance.entity.Role;
import com.example.finance.entity.User;
import com.example.finance.enums.RoleName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.EmailVerificationException;
import com.example.finance.repo.NotificationRepo;
import com.example.finance.repo.RoleRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.security.JwtService;
import com.example.finance.service.authservice.AuthServiceImpl;
import com.example.finance.service.emailverificationservice.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Test – AuthService
 * Phạm vi: Business logic – kiểm tra password, emailVerified, BCrypt, JWT generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService – Business Logic")
class AuthServiceTest {

    @Mock
    UserRepo userRepository;
    @Mock
    RoleRepo roleRepo;
    @Mock
    BCryptPasswordEncoder passwordEncoder;
    @Mock
    NotificationRepo notificationRepo; // Thêm mock này
    @Mock
    VerificationTokenService tokenService;
    @Mock
    JwtService jwtUtil;

    @InjectMocks
    AuthServiceImpl authService;

    private User verifiedUser;
    private User unverifiedUser;
    private Role role;

    @BeforeEach
    void setUp() {
        Authority authority = new Authority();
        authority.setName("CREATE");
        authority.setId(1L);

         role = new Role();
        role.setId(1L);
        role.setAuthorities(Set.of(authority));
        role.setName(RoleName.USER);

        verifiedUser = new User();
        verifiedUser.setId(1L);
        verifiedUser.setEmail("user@example.com");
        verifiedUser.setPassword("$2a$10$hashedPassword");
        verifiedUser.setFullName("Test User");
        verifiedUser.setRoles(Set.of(role));
        verifiedUser.setEnabled(true);

        unverifiedUser = new User();
        unverifiedUser.setId(2L);
        unverifiedUser.setEmail("unverified@example.com");
        unverifiedUser.setPassword("$2a$10$hashedPassword");
        verifiedUser.setRoles(Set.of(role));
        unverifiedUser.setEnabled(false);
    }

    // =========================================================
    // LOGIN – BUSINESS LOGIC
    // =========================================================
    @Nested
    @DisplayName("login() – Nghiệp vụ đăng nhập")
    class Login {

        /** TC-AUTH-LOGIN-01 – Đăng nhập thành công → trả về JWT token */
        @Test
        @DisplayName("TC-AUTH-LOGIN-01: Đăng nhập thành công trả về accessToken hợp lệ")
        void login_validCredentials_returnsTokenAndExpiry() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("abc12345", verifiedUser.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken(verifiedUser)).thenReturn("eyJhbGci...");
            when(jwtUtil.getExpiration(anyString())).thenReturn(Long.valueOf("19999"));

            LoginResponse response = authService.login(new LoginRequest("user@example.com", "abc12345"));

            assertThat(response.getToken()).isNotBlank();
            assertThat(response.getExpire()).isNotNull();
        }

        /** TC-AUTH-LOGIN-02 – Sai mật khẩu → UnauthorizedException với message chung */
        @Test
        @DisplayName("TC-AUTH-LOGIN-02: Sai mật khẩu ném UnauthorizedException, không lộ lý do")
        void login_wrongPassword_throwsUnauthorized() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("wrongpass1", verifiedUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrongpass1")))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        /** TC-AUTH-LOGIN-03 – Email không tồn tại → UnauthorizedException, CÙNG message */
        @Test
        @DisplayName("TC-AUTH-LOGIN-03: Email không tồn tại ném cùng message với sai password")
        void login_emailNotFound_throwsSameMessageAsWrongPassword() {
            when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("notexist@example.com", "abc12345")))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Invalid email or password"); // Không được khác message
        }

        /**
         * TC-AUTH-LOGIN-04 – Email chưa xác thực → ForbiddenException
         * Quan trọng: phải check password TRƯỚC, sau đó mới check emailVerified
         */
        @Test
        @DisplayName("TC-AUTH-LOGIN-04: Email chưa xác thực ném ForbiddenException sau khi pass đúng")
        void login_emailNotVerified_throwsForbidden() {
            when(userRepository.findByEmail("unverified@example.com")).thenReturn(Optional.of(unverifiedUser));
            when(passwordEncoder.matches("abc12345", unverifiedUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("unverified@example.com", "abc12345")))
                    .isInstanceOf(EmailVerificationException.class)
                    .hasMessage("Please verify your email before logging in");
        }

        /**
         * TC-AUTH-LOGIN-04b – Sai password + chưa verify → vẫn ném 401 (không lộ trạng thái)
         * Đảm bảo không bao giờ trả 403 khi sai password dù email chưa verify
         */
        @Test
        @DisplayName("TC-AUTH-LOGIN-04b: Sai password + chưa verify vẫn ném 401, không lộ trạng thái email")
        void login_wrongPasswordAndUnverified_throwsUnauthorizedNotForbidden() {
            when(userRepository.findByEmail("unverified@example.com")).thenReturn(Optional.of(unverifiedUser));
            when(passwordEncoder.matches("wrongpass1", unverifiedUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("unverified@example.com", "wrongpass1")))
                    .isInstanceOf(BadCredentialsException.class)   // 401, KHÔNG phải ForbiddenException
                    .isNotInstanceOf(EmailVerificationException.class);
        }
    }

    // =========================================================
    // REGISTER – BUSINESS LOGIC
    // =========================================================
    @Nested
    @DisplayName("register() – Nghiệp vụ đăng ký")
    class Register {

        /** TC-AUTH-REG-01 – Đăng ký thành công → trả về RegisterResponse */
        @Test
        @DisplayName("TC-AUTH-REG-01: Đăng ký thành công trả về userId, fullName, email")
        void register_validInput_returnsUserInfo() {
            when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
            when(passwordEncoder.encode("abc12345")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(123L);
                return u;
            });
            when(roleRepo.findByName(RoleName.USER)).thenReturn(Optional.of(role));

            RegisterResponse res = authService.register(
                    new RegisterRequest("Nguyễn Văn A", "user@example.com", "abc12345"));

            assertThat(res.getUserId()).isGreaterThan(0);
            assertThat(res.getFullName()).isEqualTo("Nguyễn Văn A");
            assertThat(res.getEmail()).isEqualTo("user@example.com");
        }

        /** TC-AUTH-REG-02 – Email đã tồn tại → ConflictException */
        @Test
        @DisplayName("TC-AUTH-REG-02: Email đã tồn tại ném ConflictException")
        void register_duplicateEmail_throwsConflict() {
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(
                    new RegisterRequest("Test", "existing@example.com", "abc12345")))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("Email already exist");
        }

        /**
         * TC-AUTH-REG-08 – Password phải được mã hóa BCrypt trước khi lưu
         * Đây là nghiệp vụ bảo mật quan trọng nhất của register
         */
        @Test
        @DisplayName("TC-AUTH-REG-08: Password được mã hóa BCrypt trước khi lưu vào DB")
        void register_passwordIsEncodedBeforeSave() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("abc12345")).thenReturn("$2a$10$encodedHash");
            when(roleRepo.findByName(RoleName.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });


            authService.register(new RegisterRequest("Test", "user@example.com", "abc12345"));

            // Verify encoder được gọi và save nhận password đã mã hóa
            verify(passwordEncoder).encode("abc12345");
            verify(userRepository).save(argThat(u ->
                    u.getPassword().equals("$2a$10$encodedHash") // Không phải plaintext
            ));
        }

        /** emailVerified phải là false ngay sau khi đăng ký */
        @Test
        @DisplayName("emailVerified = false ngay sau khi tạo tài khoản mới")
        void register_newUser_emailVerifiedIsFalse() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(roleRepo.findByName(RoleName.USER)).thenReturn(Optional.of(role));

            authService.register(new RegisterRequest("Test", "user@example.com", "abc12345"));

            verify(userRepository).save(argThat(u -> !u.isEnabled()));
        }
    }
}