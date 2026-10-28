package com.kieumai.notifier.presentation.auth;

import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.persistence.entity.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private final AuthService authService;

    public SecurityCurrentUserProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException("Authentication is required.");
        }
        User user = authService.findByUsername(authentication.getName());
        if (user == null) {
            throw new UnauthenticatedException("User no longer exists.");
        }
        return new CurrentUser(user.getId(), user.getUsername(), user.getLatitude(), user.getLongitude());
    }
}
