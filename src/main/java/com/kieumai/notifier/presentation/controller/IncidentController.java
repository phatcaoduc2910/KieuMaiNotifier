package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.IncidentService;
import com.kieumai.notifier.presentation.dto.IncidentDetailDto;
import com.kieumai.notifier.presentation.dto.IncidentResolveDto;
import com.kieumai.notifier.presentation.dto.IncidentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

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
            //@RequestAttribute("userId") Long currentUserId
            @RequestParam("userId") Long currentUserId) { // Lấy userId từ JWT Filter/Interceptor
        IncidentDetailDto detail = incidentService.getIncidentDetail(id, currentUserId);
        return ResponseEntity.ok(detail);
    }

    // 3. Xác nhận sự cố (Confirm)
    @PostMapping("/incidents/{id}/confirmations")
    public ResponseEntity<Void> confirmIncident(
            @PathVariable Long id,
            //@RequestAttribute("userId") Long currentUserId
            @RequestParam("userId") Long currentUserId) {
        incidentService.confirmIncident(id, currentUserId);
        return ResponseEntity.ok().build();
    }

    // 4. Hủy xác nhận sự cố
    @DeleteMapping("/incidents/{id}/confirmations/me")
    public ResponseEntity<Void> unconfirmIncident(
            @PathVariable Long id,
            //@RequestAttribute("userId") Long currentUserId
            @RequestParam("userId") Long currentUserId) {
        incidentService.unconfirmIncident(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 5. Cập nhật báo nước rút / Hết ngập sớm (RESOLVED)
    @PostMapping("/incidents/{id}/updates")
    public ResponseEntity<Void> resolveIncidentEarly(
            @PathVariable Long id,
            @RequestBody IncidentResolveDto dto,
            //@RequestAttribute("userId") Long currentUserId
            @RequestParam("userId") Long currentUserId) {
        incidentService.resolveIncidentEarly(id, dto, currentUserId);
        return ResponseEntity.ok().build();
    }
}
