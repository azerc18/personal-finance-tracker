package com.example.finance.service.emailverificationservice;

public interface VerificationTokenService {
    void createAndSendToken(String email);
    void verifyToken(String token);
    void resendToken(String email);
}
