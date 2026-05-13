    package com.example.finance.service;

    import com.example.finance.dto.notificationdto.NotificationRequest;
    import com.example.finance.dto.notificationdto.NotificationResponse;
    import com.example.finance.entity.Notification;
    import com.example.finance.entity.User;
    import com.example.finance.repo.NotificationRepo;
    import com.example.finance.repo.UserRepo;
    import com.example.finance.service.notificationservice.NotificationServiceImpl;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Nested;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.junit.jupiter.MockitoExtension;
    import java.util.Optional;
    import static org.assertj.core.api.Assertions.*;
    import static org.mockito.ArgumentMatchers.*;
    import static org.mockito.Mockito.*;

    @Nested
    @DisplayName("NotificationSettingsService – Business Logic")
    @ExtendWith(MockitoExtension.class)
    class NotificationServiceTest {

        @Mock
        NotificationRepo settingsRepository;
        @Mock
        UserRepo userRepo;
        @InjectMocks NotificationServiceImpl notificationService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);
        }

        /**
         * TC-NOTIF-PUT-04 – Setting được lưu đúng theo user_id
         */
        @Test
        @DisplayName("TC-NOTIF-PUT-04: Setting được lưu theo đúng user_id")
        void updateSettings_savedWithCorrectUserId() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Notification existing = new Notification();
            existing.setUser(user);
            existing.setDailyReminder(true);
            when(settingsRepository.findByUser(user)).thenReturn(Optional.of(existing));
            when(settingsRepository.save(any())).thenReturn(existing);

            notificationService.update(new NotificationRequest(false, true, false), user.getEmail());

            verify(settingsRepository).save(argThat(s ->
                    s.getUser().getId().equals(1L) && !s.isDailyReminder()
            ));
        }

        /**
         * TC-NOTIF-GET-01 – Trả về đúng trạng thái hiện tại
         */
        @Test
        @DisplayName("TC-NOTIF-GET-01: Trả về đúng settings hiện tại của user")
        void getSettings_returnsCurrentSettings() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Notification settings = new Notification();
            settings.setUser(user);
            settings.setDailyReminder(true);
            settings.setTipsEnabled(false);
            settings.setBudgetAlert(true);
            when(settingsRepository.findByUser(user)).thenReturn(Optional.of(settings));

            NotificationResponse res = notificationService.getNotification(user.getEmail());

            assertThat(res.isDailyReminder()).isTrue();
            assertThat(res.isTipsEnabled()).isFalse();
            assertThat(res.isBudgetAlert()).isTrue();
        }
    }