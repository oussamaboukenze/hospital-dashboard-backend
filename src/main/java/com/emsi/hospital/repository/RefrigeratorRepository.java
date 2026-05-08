package com.emsi.hospital.repository;

import com.emsi.hospital.model.Refrigerator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefrigeratorRepository extends JpaRepository<Refrigerator, Long> {
    Optional<Refrigerator> findByDeviceId(String deviceId);
}
