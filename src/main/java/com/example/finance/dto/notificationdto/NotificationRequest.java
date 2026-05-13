package com.example.finance.dto.notificationdto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    @NotNull(message = "Must be true or false")
    private boolean dailyReminder;

    @NotNull(message = "Must be true or false")
    private boolean tipsEnabled;

    @NotNull(message = "Must be true or false")
    private boolean budgetAlert;
}
