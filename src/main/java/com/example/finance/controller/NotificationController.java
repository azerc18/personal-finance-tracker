package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.notificationdto.NotificationRequest;
import com.example.finance.dto.notificationdto.NotificationResponse;
import com.example.finance.service.notificationservice.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<NotificationResponse> getNoti(@AuthenticationPrincipal UserDetails userDetails){
        NotificationResponse response = notificationService.getNotification(userDetails.getUsername());

        return new ApiResponse<>(true, "Notification settings fetched successfully", response);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE')")
    public ApiResponse<NotificationResponse> update(@Valid @RequestBody NotificationRequest request,
                                                    @AuthenticationPrincipal UserDetails userDetails){
        NotificationResponse response = notificationService.update(request, userDetails.getUsername());

        return new ApiResponse<>(true, "Notification settings updated successfully", response);
    }
}
