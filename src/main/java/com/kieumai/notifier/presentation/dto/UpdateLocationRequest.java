package com.kieumai.notifier.presentation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateLocationRequest(
        @NotNull(message = "latitude is required.")
        @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90.")
        @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90.")
        @Digits(integer = 3, fraction = 7, message = "latitude must have at most 7 decimal places.")
        BigDecimal latitude,

        @NotNull(message = "longitude is required.")
        @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180.")
        @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180.")
        @Digits(integer = 3, fraction = 7, message = "longitude must have at most 7 decimal places.")
        BigDecimal longitude
) {
}
