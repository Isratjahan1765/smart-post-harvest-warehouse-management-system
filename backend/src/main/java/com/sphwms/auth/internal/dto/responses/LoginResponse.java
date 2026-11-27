package com.sphwms.auth.internal.dto.responses;

public record LoginResponse(String token, long expiresInSeconds, UserInfo user) {

    public record UserInfo(Long userId, String username, String name, String fullName,
                           String role, String email, String phone) {
    }
}
