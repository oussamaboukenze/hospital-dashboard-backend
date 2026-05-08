package com.emsi.hospital.controller;

import com.emsi.hospital.dto.ReadingResponse;
import com.emsi.hospital.dto.RefrigeratorResponse;
import com.emsi.hospital.dto.TelemetryMessage;
import com.emsi.hospital.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RefrigeratorController {

    private final TelemetryService telemetryService;

    public RefrigeratorController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/fridges")
    public List<RefrigeratorResponse> getRefrigerators() {
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
}
