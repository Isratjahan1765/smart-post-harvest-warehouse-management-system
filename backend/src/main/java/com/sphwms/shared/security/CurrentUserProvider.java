package com.sphwms.shared.security;

import org.springframework.stereotype.Component;

/**
 * Provides the current authenticated user id.
 * While login is not yet implemented, always returns user id 1 (the manager).
 */
@Component
public class CurrentUserProvider {

    public Long getCurrentUserId() {
        return 1L;
    }
}
