package com.example.finance.service.notificationservice;

import com.example.finance.dto.notificationdto.NotificationRequest;
import com.example.finance.dto.notificationdto.NotificationResponse;
import com.example.finance.entity.Notification;
import com.example.finance.entity.User;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.NotificationRepo;
import com.example.finance.repo.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    @Override
    public NotificationResponse getNotification(String email) {
        User user = getUser(email);

        Notification notification = getNotificationByUser(user);

        return toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse update(NotificationRequest request, String email) {
        User user = getUser(email);

        Notification notification = getNotificationByUser(user);

        notification.setBudgetAlert(request.isBudgetAlert());
        notification.setTipsEnabled(request.isTipsEnabled());
        notification.setDailyReminder(request.isDailyReminder());

        Notification saved = notificationRepo.save(notification);

        return toResponse(saved);
    }

    private User getUser(String email){
        return userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Email not found"));
    }

    private Notification getNotificationByUser(User user){
         return notificationRepo.findByUser(user)
                .orElseThrow(()-> new ResourceNotFoundException("Notification not found"));
    }

    private NotificationResponse toResponse(Notification notification){
        return NotificationResponse.builder()
                .budgetAlert(notification.isBudgetAlert())
                .dailyReminder(notification.isDailyReminder())
                .tipsEnabled(notification.isTipsEnabled())
                .build();
    }
}
