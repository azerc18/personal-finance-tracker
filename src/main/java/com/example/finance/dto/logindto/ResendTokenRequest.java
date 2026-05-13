package com.example.finance.dto.logindto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResendTokenRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
}
