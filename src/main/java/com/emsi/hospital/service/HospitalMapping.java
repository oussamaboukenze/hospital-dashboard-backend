package com.emsi.hospital.service;

import com.emsi.hospital.dto.AlertResponse;
import com.emsi.hospital.dto.ReadingResponse;
import com.emsi.hospital.dto.RefrigeratorResponse;
import com.emsi.hospital.model.Alert;
import com.emsi.hospital.model.Refrigerator;
import com.emsi.hospital.model.SensorReading;

final class HospitalMapping {

    private HospitalMapping() {
    }

    static RefrigeratorResponse toRefrigeratorResponse(Refrigerator refrigerator) {
        return new RefrigeratorResponse(
                refrigerator.getId(),
                refrigerator.getDeviceId(),
                refrigerator.getLabel(),
                refrigerator.getLocation(),
                refrigerator.getMinTemperature(),
                refrigerator.getMaxTemperature(),
                refrigerator.getLastTemperature(),
                refrigerator.getLastHumidity(),
                refrigerator.getDoorOpen(),
                refrigerator.getBattery(),
                refrigerator.getLastSeenAt(),
                refrigerator.getStatus()
        );
    }

    static ReadingResponse toReadingResponse(SensorReading reading) {
        return new ReadingResponse(
                reading.getId(),
                reading.getDeviceId(),
                reading.getTemperature(),
                reading.getHumidity(),
                reading.isDoorOpen(),
                reading.getBattery(),
                reading.getRecordedAt()
        );
    }

    static AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getDeviceId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.isResolved()
        );
    }
}
