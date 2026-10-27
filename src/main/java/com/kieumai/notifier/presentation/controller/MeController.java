package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.presentation.auth.CurrentUserProvider;
import com.kieumai.notifier.presentation.dto.MeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentUserProvider currentUserProvider;

    public MeController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public MeResponse getMe() {
        return MeResponse.from(currentUserProvider.currentUser());
    }
}
