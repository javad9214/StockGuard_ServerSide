package com.stockguard.data.dto.auth.response;

import com.stockguard.data.dto.auth.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token; // Access token
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserDTO user;
    private String message;
}