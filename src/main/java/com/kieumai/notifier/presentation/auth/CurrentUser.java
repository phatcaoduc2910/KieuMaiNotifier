package com.kieumai.notifier.presentation.auth;

import java.math.BigDecimal;

public record CurrentUser(
        long userId,
        String username,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public CurrentUser(long userId, String username) {
        this(userId, username, null, null);
    }
}
