package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.business.model.Ward;

public record WardResponse(
        long id,
        String code,
        String name,
        String provinceName
) {

    public static WardResponse from(Ward ward) {
        return new WardResponse(ward.id(), ward.code(), ward.name(), ward.provinceName());
    }
}
