package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.IncidentService;
import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.presentation.dto.IncidentDetailDto;
import com.kieumai.notifier.presentation.dto.IncidentResolveDto;
import com.kieumai.notifier.presentation.dto.IncidentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final AuthService authService;

    // 1. Xem danh sách sự cố (lọc theo phường và loại sự cố)
    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponseDto>> getIncidents(
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) String type) {
        List<IncidentResponseDto> incidents = incidentService.getIncidents(wardId, type);
        return ResponseEntity.ok(incidents);
    }

    // 2. Xem chi tiết 1 sự cố
    @GetMapping("/incidents/{id}")
    public ResponseEntity<IncidentDetailDto> getIncidentDetail(
            @PathVariable Long id,
            Authentication authentication) {
        IncidentDetailDto detail = incidentService.getIncidentDetail(id, currentUserId(authentication));
        return ResponseEntity.ok(detail);
    }

    // 3. Xác nhận sự cố (Confirm)
    @PostMapping("/incidents/{id}/confirmations")
    public ResponseEntity<Void> confirmIncident(
            @PathVariable Long id,
            Authentication authentication) {
        incidentService.confirmIncident(id, currentUserId(authentication));
        return ResponseEntity.ok().build();
    }

    // 4. Hủy xác nhận sự cố
    @DeleteMapping("/incidents/{id}/confirmations/me")
    public ResponseEntity<Void> unconfirmIncident(
            @PathVariable Long id,
            Authentication authentication) {
        incidentService.unconfirmIncident(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    // 5. Cập nhật báo nước rút / Hết ngập sớm (RESOLVED)
    @PostMapping("/incidents/{id}/updates")
    public ResponseEntity<Void> resolveIncidentEarly(
            @PathVariable Long id,
            @RequestBody IncidentResolveDto dto,
            Authentication authentication) {
        incidentService.resolveIncidentEarly(id, dto, currentUserId(authentication));
        return ResponseEntity.ok().build();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = authService.findByUsername(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}
