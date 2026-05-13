package com.example.finance.dto.profiledto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^https?://.*.(png|jpg|jpeg|gif|webp)$",
             message = "Invalid avatar URL" )
    private String avatar;
}
