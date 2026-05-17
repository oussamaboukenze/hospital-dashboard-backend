package com.emsi.hospital.controller;

import com.emsi.hospital.dto.ReadingResponse;
import com.emsi.hospital.service.TelemetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final TelemetryService telemetryService;

    public HistoryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public List<ReadingResponse> getHistory(
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return telemetryService.getHistory(deviceId, limit);
    }
}
