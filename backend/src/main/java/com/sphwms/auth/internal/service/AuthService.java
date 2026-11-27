package com.sphwms.auth.internal.service;

import com.sphwms.auth.internal.dto.requests.LoginRequest;
import com.sphwms.auth.internal.dto.responses.LoginResponse;
import com.sphwms.auth.internal.dto.responses.LoginResponse.UserInfo;
import com.sphwms.user.internal.entity.AppUser;
import com.sphwms.user.internal.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String INVALID = "Invalid username or password.";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest req) {
        AppUser user = users.findByUsername(req.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account locked after 5 failed attempts. Please contact the administrator.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus("LOCKED");
            }
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID);
        }

        user.setFailedAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        users.save(user);

        String token = jwtService.generate(user.getUsername(), normalizeRole(user.getRole()));
        return new LoginResponse(token, expirationMs / 1000, toUserInfo(user));
    }

    public UserInfo me(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return toUserInfo(user);
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase().replace(' ', '_');
    }

    private static UserInfo toUserInfo(AppUser u) {
        return new UserInfo(u.getUserId(), u.getUsername(), u.getFullName(), u.getFullName(),
                normalizeRole(u.getRole()),
                u.getEmail() != null ? u.getEmail() : "",
                u.getPhone() != null ? u.getPhone() : "");
    }
}
