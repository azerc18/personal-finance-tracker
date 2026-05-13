    package com.example.finance.service;

    import com.example.finance.dto.profiledto.ProfileRequest;
    import com.example.finance.dto.profiledto.ProfileResponse;
    import com.example.finance.entity.User;
    import com.example.finance.repo.UserRepo;
    import com.example.finance.service.userservice.UserServiceImpl;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Nested;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.junit.jupiter.MockitoExtension;

    import java.util.Optional;

    import static org.assertj.core.api.Assertions.assertThat;
    import static org.mockito.ArgumentMatchers.any;
    import static org.mockito.ArgumentMatchers.argThat;
    import static org.mockito.Mockito.verify;
    import static org.mockito.Mockito.when;

    @Nested
    @DisplayName("UserProfileService – Business Logic")
    @ExtendWith(MockitoExtension.class)
    public class ProfileServiceTest {
        @Mock
        UserRepo userRepository;
        @InjectMocks
        UserServiceImpl userProfileService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);
            user.setEmail("user@example.com");
            user.setFullName("Old Name");
        }

        /**
         * TC-PROFILE-01 – Cập nhật thành công, email và userId không thay đổi
         */
        @Test
        @DisplayName("TC-PROFILE-01: email và userId không bị thay đổi sau khi update profile")
        void updateProfile_emailAndUserIdUnchanged() {
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            ProfileRequest req = new ProfileRequest("Nguyễn Văn A",
                    "https://cdn.example.com/avatar.png");
            ProfileResponse res = userProfileService.updateProfile(req, user.getEmail());

            assertThat(res.getUserId()).isEqualTo(1L);
            assertThat(res.getEmail()).isEqualTo("user@example.com"); // Không thay đổi
            assertThat(res.getFullName()).isEqualTo("Nguyễn Văn A");  // Đã cập nhật
        }

        /**
         * TC-PROFILE-06 – Chỉ update thông tin của user hiện tại (save đúng entity)
         */
        @Test
        @DisplayName("TC-PROFILE-06: Chỉ lưu thông tin của user đang đăng nhập")
        void updateProfile_savesCorrectUser() {
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            userProfileService.updateProfile(
                    new ProfileRequest("New Name", null), user.getEmail());

            verify(userRepository).save(argThat(u -> u.getId().equals(1L)));
        }
    }
