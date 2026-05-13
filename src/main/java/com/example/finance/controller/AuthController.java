package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.logindto.LoginRequest;
import com.example.finance.dto.logindto.LoginResponse;
import com.example.finance.dto.logindto.ResendTokenRequest;
import com.example.finance.dto.registerdto.RegisterRequest;
import com.example.finance.dto.registerdto.RegisterResponse;
import com.example.finance.service.authservice.AuthService;
import com.example.finance.service.emailverificationservice.VerificationTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final VerificationTokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
            RegisterResponse response = authService.register(request);
        return new ResponseEntity<>(new ApiResponse<>(true,  "Register successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return new ApiResponse<>(true,  "Login successfully", response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token){
        tokenService.verifyToken(token);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification email resent successfully"
        ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody @Valid ResendTokenRequest request) {
        tokenService.resendToken(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification email resent successfully"
        ));
    }
}
