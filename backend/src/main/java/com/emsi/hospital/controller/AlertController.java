package com.emsi.hospital.controller;

import com.emsi.hospital.dto.AlertResponse;
import com.emsi.hospital.model.AppUser;
import com.emsi.hospital.model.Refrigerator;
import com.emsi.hospital.repository.AppUserRepository;
import com.emsi.hospital.service.AlertService;
import com.emsi.hospital.util.TokenUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final AppUserRepository userRepository;

    public AlertController(AlertService alertService, AppUserRepository userRepository) {
        this.alertService = alertService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AlertResponse> getAlerts(
            @RequestParam(required = false) Boolean resolved,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = TokenUtils.extractUserId(auth);
        if (userId != null) {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user != null && "technician".equals(user.getRole())) {
                List<String> deviceIds = user.getAssignedFridges().stream()
                        .map(Refrigerator::getDeviceId).toList();
                return alertService.getAlertsForDevices(resolved, deviceIds);
            }
        }
        return alertService.getAlerts(resolved);
    }

    @PatchMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable Long id) {
        return alertService.resolve(id);
    }
}
