package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.presentation.auth.CurrentUser;

public record MeResponse(
        long userId,
        String username
) {

    public static MeResponse from(CurrentUser user) {
        return new MeResponse(user.userId(), user.username());
    }
}
