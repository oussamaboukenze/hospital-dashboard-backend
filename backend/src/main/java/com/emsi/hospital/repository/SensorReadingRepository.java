package com.emsi.hospital.repository;

import com.emsi.hospital.model.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findByDeviceIdOrderByRecordedAtDesc(String deviceId, Pageable pageable);

    List<SensorReading> findAllByOrderByRecordedAtDesc(Pageable pageable);
}
