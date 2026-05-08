package com.emsi.hospital.service;

import com.emsi.hospital.dto.AlertResponse;
import com.emsi.hospital.model.Alert;
import com.emsi.hospital.repository.AlertRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(AlertRepository alertRepository, SimpMessagingTemplate messagingTemplate) {
        this.alertRepository = alertRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(Boolean resolved) {
        List<Alert> alerts = resolved == null
                ? alertRepository.findAllByOrderByCreatedAtDesc()
                : alertRepository.findByResolvedOrderByCreatedAtDesc(resolved);
        return alerts.stream().map(HospitalMapping::toAlertResponse).toList();
    }

    @Transactional
    public AlertResponse resolve(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setResolved(true);
        AlertResponse response = HospitalMapping.toAlertResponse(alertRepository.save(alert));
        messagingTemplate.convertAndSend("/topic/alerts", response);
        return response;
    }
}
