package com.example.finance.service.userservice;

import com.example.finance.dto.profiledto.ProfileRequest;
import com.example.finance.dto.profiledto.ProfileResponse;
import com.example.finance.entity.User;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;

    @Override
    public ProfileResponse updateProfile(ProfileRequest request, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Email not found"));

        user.setAvatar(request.getAvatar());
        user.setFullName(request.getFullName());

        User saved = userRepo.save(user);

        return toResponse(saved);
    }

    private ProfileResponse toResponse(User user){
        return new ProfileResponse(user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatar());
    }

}
