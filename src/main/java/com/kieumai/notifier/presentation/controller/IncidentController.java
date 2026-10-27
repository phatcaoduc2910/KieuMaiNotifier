package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.exception.ReportException;
import com.kieumai.notifier.business.service.*;
import com.kieumai.notifier.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final AuthService authService;

    @GetMapping("/incidents")
    public List<IncidentResponseDto> getIncidents(
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) String type) {
        return incidentService.getIncidents(wardId, type);
    }

    @GetMapping("/incidents/{id}")
    public IncidentDetailDto getIncidentDetail(@PathVariable Long id) {
        return incidentService.getIncidentDetail(id);
    }

    @PostMapping("/incidents/{id}/updates")
    public ResponseEntity<Void> resolveIncidentEarly(@PathVariable Long id,
                                                    Authentication authentication) {
        var user = authentication == null ? null : authService.findByUsername(authentication.getName());
        if (user == null) throw new ReportException(ReportException.Code.UNAUTHORIZED, "Authentication is required.");
        incidentService.resolveIncidentEarly(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/incidents/feed")
    public ResponseEntity<List<IncidentResponseDto>> getFeed(
            @RequestParam("userId") Long userId) {
        List<IncidentResponseDto> feed = incidentService.getFeed(userId);
        return ResponseEntity.ok(feed);
    }
}
