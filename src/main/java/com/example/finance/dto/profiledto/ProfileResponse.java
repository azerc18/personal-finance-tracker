package com.example.finance.dto.profiledto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatar;
}
