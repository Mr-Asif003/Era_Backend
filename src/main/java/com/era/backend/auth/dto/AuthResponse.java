package com.era.backend.auth.dto;

import com.era.backend.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private User user;
    private String accessToken;
    private String refreshToken;
}
