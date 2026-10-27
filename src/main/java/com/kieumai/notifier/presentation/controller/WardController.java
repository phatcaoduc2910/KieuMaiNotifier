package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.WardService;
import com.kieumai.notifier.presentation.dto.WardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wards")
public class WardController {

    private final WardService wardService;

    public WardController(WardService wardService) {
        this.wardService = wardService;
    }

    @GetMapping
    public List<WardResponse> getWards() {
        return wardService.getWards().stream()
                .map(WardResponse::from)
                .toList();
    }
}
