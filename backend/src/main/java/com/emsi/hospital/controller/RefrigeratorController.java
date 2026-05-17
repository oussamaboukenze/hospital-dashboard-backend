package com.emsi.hospital.controller;

import com.emsi.hospital.dto.ReadingResponse;
import com.emsi.hospital.dto.RefrigeratorResponse;
import com.emsi.hospital.dto.TelemetryMessage;
import com.emsi.hospital.model.AppUser;
import com.emsi.hospital.model.Refrigerator;
import com.emsi.hospital.repository.AppUserRepository;
import com.emsi.hospital.service.TelemetryService;
import com.emsi.hospital.util.TokenUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RefrigeratorController {

    private final TelemetryService telemetryService;
    private final AppUserRepository userRepository;

    public RefrigeratorController(TelemetryService telemetryService, AppUserRepository userRepository) {
        this.telemetryService = telemetryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/fridges")
    public List<RefrigeratorResponse> getRefrigerators(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        List<String> assigned = getTechnicianFridgeIds(auth);
        if (assigned != null) return telemetryService.getRefrigeratorsForDeviceIds(assigned);
        return telemetryService.getRefrigerators();
    }

    @GetMapping("/fridges/{deviceId}")
    public RefrigeratorResponse getRefrigerator(@PathVariable String deviceId) {
        return telemetryService.getRefrigerator(deviceId);
    }

    @GetMapping("/fridges/{deviceId}/readings")
    public List<ReadingResponse> getReadings(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "80") int limit
    ) {
        return telemetryService.getReadings(deviceId, limit);
    }

    @GetMapping("/readings/latest")
    public List<ReadingResponse> getLatestReadings(@RequestParam(defaultValue = "50") int limit) {
        return telemetryService.getLatestReadings(limit);
    }

    @PostMapping("/readings/simulate")
    public ReadingResponse simulateReading(@Valid @RequestBody TelemetryMessage message) {
        return telemetryService.ingest(message, "manual-rest-simulation");
    }

    private List<String> getTechnicianFridgeIds(String auth) {
        Long userId = TokenUtils.extractUserId(auth);
        if (userId == null) return null;
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || !"technician".equals(user.getRole())) return null;
        return user.getAssignedFridges().stream().map(Refrigerator::getDeviceId).toList();
    }
}
