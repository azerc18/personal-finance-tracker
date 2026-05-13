package com.example.finance.service.userservice;

import com.example.finance.dto.profiledto.ProfileRequest;
import com.example.finance.dto.profiledto.ProfileResponse;

public interface UserService {
    ProfileResponse updateProfile(ProfileRequest request, String email);
}
