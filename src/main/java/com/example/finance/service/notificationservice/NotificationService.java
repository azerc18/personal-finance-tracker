package com.example.finance.service.notificationservice;

import com.example.finance.dto.notificationdto.NotificationRequest;
import com.example.finance.dto.notificationdto.NotificationResponse;

public interface NotificationService {
    NotificationResponse getNotification(String email);
    NotificationResponse update(NotificationRequest request, String email);
}
