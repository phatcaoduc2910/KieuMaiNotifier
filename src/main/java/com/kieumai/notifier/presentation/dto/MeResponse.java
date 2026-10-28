package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.presentation.auth.CurrentUser;

import java.math.BigDecimal;

public record MeResponse(
        long userId,
        String username,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public static MeResponse from(CurrentUser user) {
        return new MeResponse(user.userId(), user.username(), user.latitude(), user.longitude());
    }
}
