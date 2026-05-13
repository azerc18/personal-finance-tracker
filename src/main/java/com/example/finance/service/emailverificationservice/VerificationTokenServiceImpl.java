package com.example.finance.service.emailverificationservice;

import com.example.finance.entity.User;
import com.example.finance.entity.VerificationToken;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.EmailVerifyTokenRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.utils.EmailGenerator;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService{
    private final UserRepo userRepo;
    private final EmailGenerator emailGenerator;
    private final EmailVerifyTokenRepo emailVerifyTokenRepo;

    @Value("${app.jwt.expiration-seconds}")
    private int tokenExpiryHours;

    @Override
    @Transactional
    public void createAndSendToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));

        VerificationToken token = emailVerifyTokenRepo.findByUser(user)
                .orElse(new VerificationToken());

        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpire(LocalDateTime.now().plusHours(tokenExpiryHours));
        emailVerifyTokenRepo.save(token);

        try {
            emailGenerator.sendVerificationEmail(email, token.getToken());
        } catch (MessagingException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyToken(String token) {
        VerificationToken verificationToken = emailVerifyTokenRepo.findByToken(token)
                .orElseThrow(()-> new ResourceNotFoundException("Invalid or expired verification token"));

        if (verificationToken.getExpire().isBefore(LocalDateTime.now())) {
            throw new BusinessConflictException("Invalid or expired verification token");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);

        emailVerifyTokenRepo.delete(verificationToken);
    }

    @Override
    @Transactional
    public void resendToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        if (user.isEnabled()) {
            throw new BusinessConflictException("Email is already verified");
        }

        createAndSendToken(user.getEmail());
    }
}
