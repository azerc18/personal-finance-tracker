package com.example.finance.service.authservice;


import com.example.finance.dto.logindto.LoginRequest;
import com.example.finance.dto.logindto.LoginResponse;
import com.example.finance.dto.registerdto.RegisterRequest;
import com.example.finance.dto.registerdto.RegisterResponse;


public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
