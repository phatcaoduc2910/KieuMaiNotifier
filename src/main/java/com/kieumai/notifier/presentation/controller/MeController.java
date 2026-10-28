package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.presentation.auth.CurrentUserProvider;
import com.kieumai.notifier.presentation.dto.MeResponse;
import com.kieumai.notifier.presentation.dto.UpdateLocationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentUserProvider currentUserProvider;
    private final AuthService authService;

    public MeController(CurrentUserProvider currentUserProvider, AuthService authService) {
        this.currentUserProvider = currentUserProvider;
        this.authService = authService;
    }

    @GetMapping("/me")
    public MeResponse getMe() {
        return MeResponse.from(currentUserProvider.currentUser());
    }

    @PutMapping("/me/location")
    public MeResponse updateLocation(@Valid @RequestBody UpdateLocationRequest request) {
        long userId = currentUserProvider.currentUserId();
        User updated = authService.updateLocation(userId, request.latitude(), request.longitude());
        return new MeResponse(updated.getId(), updated.getUsername(), updated.getLatitude(), updated.getLongitude());
    }
}
