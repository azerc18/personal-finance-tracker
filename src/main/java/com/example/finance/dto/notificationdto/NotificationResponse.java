package com.example.finance.dto.notificationdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private boolean dailyReminder;
    private boolean tipsEnabled;
    private boolean budgetAlert;
}
