package com.kieumai.notifier.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(@NotNull @Positive Long wardId) {
}
