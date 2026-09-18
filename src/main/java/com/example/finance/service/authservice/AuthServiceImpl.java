package com.example.finance.service.authservice;


import com.example.finance.dto.logindto.LoginRequest;
import com.example.finance.dto.logindto.LoginResponse;
import com.example.finance.dto.registerdto.RegisterRequest;
import com.example.finance.dto.registerdto.RegisterResponse;
import com.example.finance.entity.Notification;
import com.example.finance.entity.Role;
import com.example.finance.entity.User;
import com.example.finance.enums.RoleName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.EmailVerificationException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.NotificationRepo;
import com.example.finance.repo.RoleRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.security.JwtService;
import com.example.finance.service.emailverificationservice.VerificationTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationRepo notificationRepo;
    private final VerificationTokenService tokenService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        //Kiểm tra email đã tồn tại
        if(userRepo.existsByEmail(request.getEmail())){
            throw new BusinessConflictException("Email already exist");
        }

        //Tạo người dùng mới
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        user.setCreated_at(LocalDate.now());

        userRepo.save(user);
        //Kiểm tra role và gán role mặc định là USER
        Role userRole = roleRepo.findByName(RoleName.USER)
                        .orElseThrow(()-> new ResourceNotFoundException("USER is not found"));
        user.setRoles(Set.of(userRole));

        //Tao default noti
        Notification noti = new Notification();
        noti.setUser(user);
        noti.setDailyReminder(false);
        noti.setTipsEnabled(false);
        noti.setBudgetAlert(true);
        notificationRepo.save(noti);

        tokenService.createAndSendToken(request.getEmail());

        //Trả Response cho Controller
        return new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        //Kểm tra email đã tồn tại
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(()-> new UsernameNotFoundException("Invalid email or password"));

        //Kiểm tra password có match không
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new BadCredentialsException("Invalid email or password");
        }

        if(!user.isEnabled()){
            throw new EmailVerificationException("Please verify your email before logging in");
        }

        //Tạo token
        String token = jwtService.generateToken(user);
        Long expire = jwtService.getExpiration(token);

        //Trả Response cho Controller
        return new LoginResponse(token, expire);
    }
}
