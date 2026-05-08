package com.emsi.hospital.service;

import com.emsi.hospital.dto.AlertResponse;
import com.emsi.hospital.dto.ReadingResponse;
import com.emsi.hospital.dto.RefrigeratorResponse;
import com.emsi.hospital.dto.TelemetryMessage;
import com.emsi.hospital.model.Alert;
import com.emsi.hospital.model.Refrigerator;
import com.emsi.hospital.model.SensorReading;
import com.emsi.hospital.repository.AlertRepository;
import com.emsi.hospital.repository.RefrigeratorRepository;
import com.emsi.hospital.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryService {

    private static final Map<String, String> DEFAULT_LOCATIONS = Map.of(
            "FRIDGE-001", "Pharmacie centrale",
            "FRIDGE-002", "Bloc operatoire",
            "FRIDGE-003", "Laboratoire",
            "FRIDGE-004", "Pediatrie",
            "FRIDGE-005", "Urgences"
    );

    private final RefrigeratorRepository refrigeratorRepository;
    private final SensorReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TelemetryService(
            RefrigeratorRepository refrigeratorRepository,
            SensorReadingRepository readingRepository,
            AlertRepository alertRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.refrigeratorRepository = refrigeratorRepository;
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ReadingResponse ingest(TelemetryMessage message, String sourceTopic) {
        Instant recordedAt = parseTimestamp(message.getTimestamp());
        Refrigerator refrigerator = refrigeratorRepository.findByDeviceId(message.getDeviceId())
                .orElseGet(() -> createDefaultRefrigerator(message.getDeviceId()));

        SensorReading reading = new SensorReading();
        reading.setDeviceId(message.getDeviceId());
        reading.setTemperature(message.getTemperature());
        reading.setHumidity(message.getHumidity());
        reading.setDoorOpen(Boolean.TRUE.equals(message.getDoorOpen()));
        reading.setBattery(message.getBattery() == null ? 100 : message.getBattery());
        reading.setRecordedAt(recordedAt);
        reading.setSourceTopic(sourceTopic);
        SensorReading savedReading = readingRepository.save(reading);

        refrigerator.setLastTemperature(reading.getTemperature());
        refrigerator.setLastHumidity(reading.getHumidity());
        refrigerator.setDoorOpen(reading.isDoorOpen());
        refrigerator.setBattery(reading.getBattery());
        refrigerator.setLastSeenAt(recordedAt);
        refrigerator.setStatus(computeStatus(refrigerator, reading));
        Refrigerator savedRefrigerator = refrigeratorRepository.save(refrigerator);

        List<AlertResponse> createdAlerts = createAlertsIfNeeded(savedRefrigerator, reading);
        ReadingResponse readingResponse = HospitalMapping.toReadingResponse(savedReading);
        RefrigeratorResponse refrigeratorResponse = HospitalMapping.toRefrigeratorResponse(savedRefrigerator);

        messagingTemplate.convertAndSend("/topic/readings", readingResponse);
        messagingTemplate.convertAndSend("/topic/fridges", refrigeratorResponse);
        createdAlerts.forEach(alert -> messagingTemplate.convertAndSend("/topic/alerts", alert));

        return readingResponse;
    }

    @Transactional(readOnly = true)
    public List<RefrigeratorResponse> getRefrigerators() {
        return refrigeratorRepository.findAll().stream()
                .map(HospitalMapping::toRefrigeratorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefrigeratorResponse getRefrigerator(String deviceId) {
        return refrigeratorRepository.findByDeviceId(deviceId)
                .map(HospitalMapping::toRefrigeratorResponse)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<ReadingResponse> getReadings(String deviceId, int limit) {
        List<SensorReading> readings = readingRepository.findByDeviceIdOrderByRecordedAtDesc(
                deviceId,
                PageRequest.of(0, Math.min(limit, 500))
        );
        Collections.reverse(readings);
        return readings.stream().map(HospitalMapping::toReadingResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingResponse> getLatestReadings(int limit) {
        return readingRepository.findAllByOrderByRecordedAtDesc(PageRequest.of(0, Math.min(limit, 500)))
                .stream()
                .map(HospitalMapping::toReadingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingResponse> getHistory(String deviceId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        if (deviceId == null || deviceId.isBlank()) {
            return getLatestReadings(safeLimit);
        }
        return getReadings(deviceId, safeLimit);
    }

    private Refrigerator createDefaultRefrigerator(String deviceId) {
        Refrigerator refrigerator = new Refrigerator();
        refrigerator.setDeviceId(deviceId);
        refrigerator.setLabel("Refrigerateur " + readableLocation(deviceId));
        refrigerator.setLocation(DEFAULT_LOCATIONS.getOrDefault(deviceId, "Service non renseigne"));
        refrigerator.setMinTemperature(2.0);
        refrigerator.setMaxTemperature(8.0);
        refrigerator.setStatus("UNKNOWN");
        return refrigeratorRepository.save(refrigerator);
    }

    private String readableLocation(String deviceId) {
        return DEFAULT_LOCATIONS.getOrDefault(deviceId, deviceId);
    }

    private Instant parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException ignored) {
            return Instant.now();
        }
    }

    private String computeStatus(Refrigerator refrigerator, SensorReading reading) {
        boolean outsideRange = reading.getTemperature() < refrigerator.getMinTemperature()
                || reading.getTemperature() > refrigerator.getMaxTemperature();
        if (reading.isDoorOpen() || outsideRange) {
            double distance = Math.max(
                    refrigerator.getMinTemperature() - reading.getTemperature(),
                    reading.getTemperature() - refrigerator.getMaxTemperature()
            );
            return distance > 2.0 || reading.isDoorOpen() ? "CRITICAL" : "WARNING";
        }
        return "OK";
    }

    private List<AlertResponse> createAlertsIfNeeded(Refrigerator refrigerator, SensorReading reading) {
        List<AlertResponse> alerts = new ArrayList<>();
        if (reading.getTemperature() < refrigerator.getMinTemperature()
                || reading.getTemperature() > refrigerator.getMaxTemperature()) {
            alerts.add(createAlertOnce(
                    reading.getDeviceId(),
                    "TEMPERATURE",
                    refrigerator.getStatus(),
                    "Temperature hors plage: %.1f C (attendu %.1f C a %.1f C)"
                            .formatted(reading.getTemperature(), refrigerator.getMinTemperature(), refrigerator.getMaxTemperature())
            ));
        }
        if (reading.isDoorOpen()) {
            alerts.add(createAlertOnce(
                    reading.getDeviceId(),
                    "DOOR_OPEN",
                    "CRITICAL",
                    "Porte detectee ouverte sur " + refrigerator.getLabel()
            ));
        }
        return alerts;
    }

    private AlertResponse createAlertOnce(String deviceId, String type, String severity, String message) {
        Alert alert = alertRepository.findTopByDeviceIdAndTypeAndResolvedFalseOrderByCreatedAtDesc(deviceId, type)
                .orElseGet(Alert::new);
        alert.setDeviceId(deviceId);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setCreatedAt(alert.getCreatedAt() == null ? Instant.now() : alert.getCreatedAt());
        alert.setResolved(false);
        return HospitalMapping.toAlertResponse(alertRepository.save(alert));
    }
}
