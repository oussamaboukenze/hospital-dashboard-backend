package com.emsi.hospital.repository;

import com.emsi.hospital.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByResolvedOrderByCreatedAtDesc(boolean resolved);

    List<Alert> findAllByOrderByCreatedAtDesc();

    Optional<Alert> findTopByDeviceIdAndTypeAndResolvedFalseOrderByCreatedAtDesc(String deviceId, String type);
}
