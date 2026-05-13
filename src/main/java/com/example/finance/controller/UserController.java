package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.profiledto.ProfileRequest;
import com.example.finance.dto.profiledto.ProfileResponse;
import com.example.finance.service.userservice.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE')")
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails){

        ProfileResponse response = userService.updateProfile(request, userDetails.getUsername());

        return new ApiResponse<>(true, "User profile updated successfully", response);
    }
}
