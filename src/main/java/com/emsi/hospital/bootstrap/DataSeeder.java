package com.emsi.hospital.bootstrap;

import com.emsi.hospital.dto.TelemetryMessage;
import com.emsi.hospital.model.AppUser;
import com.emsi.hospital.repository.AppUserRepository;
import com.emsi.hospital.repository.RefrigeratorRepository;
import com.emsi.hospital.service.TelemetryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RefrigeratorRepository refrigeratorRepository;
    private final AppUserRepository userRepository;
    private final TelemetryService telemetryService;

    public DataSeeder(
            RefrigeratorRepository refrigeratorRepository,
            AppUserRepository userRepository,
            TelemetryService telemetryService
    ) {
        this.refrigeratorRepository = refrigeratorRepository;
        this.userRepository = userRepository;
        this.telemetryService = telemetryService;
    }

    @Override
    public void run(String... args) {
        seedUser("admin@hospital.com", "admin123", "Admin EMSI", "admin");
        seedUser("technicien@hospital.com", "tech123", "Technicien Bloc A", "technician");

        List<TelemetryMessage> seedReadings = List.of(
                message("FRIDGE-001", 4.2, 57.0, false, 96),
                message("FRIDGE-002", 7.4, 60.0, false, 88),
                message("FRIDGE-003", 9.1, 64.5, false, 77),
                message("FRIDGE-004", 3.6, 53.0, false, 91),
                message("FRIDGE-005", 5.8, 58.0, false, 84)
        );
        seedReadings.stream()
                .filter(message -> refrigeratorRepository.findByDeviceId(message.getDeviceId()).isEmpty())
                .forEach(message -> telemetryService.ingest(message, "seed"));
    }

    private TelemetryMessage message(String deviceId, double temperature, double humidity, boolean doorOpen, int battery) {
        TelemetryMessage message = new TelemetryMessage();
        message.setDeviceId(deviceId);
        message.setTemperature(temperature);
        message.setHumidity(humidity);
        message.setDoorOpen(doorOpen);
        message.setBattery(battery);
        return message;
    }

    private void seedUser(String email, String password, String name, String role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);
        userRepository.save(user);
    }
}
