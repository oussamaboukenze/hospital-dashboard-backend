package com.emsi.hospital.service;

import com.emsi.hospital.dto.TelemetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttSubscriber implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttSubscriber.class);

    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private final String topic;
    private final int connectionTimeoutSeconds;
    private final int keepAliveSeconds;
    private MqttClient client;

    public MqttSubscriber(
            TelemetryService telemetryService,
            ObjectMapper objectMapper,
            @Value("${mqtt.broker-url}") String brokerUrl,
            @Value("${mqtt.client-id}") String clientId,
            @Value("${mqtt.username}") String username,
            @Value("${mqtt.password}") String password,
            @Value("${mqtt.topic}") String topic,
            @Value("${mqtt.connection-timeout-seconds}") int connectionTimeoutSeconds,
            @Value("${mqtt.keep-alive-seconds}") int keepAliveSeconds
    ) {
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @PostConstruct
    public void connect() {
        try {
            client = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis(), new MemoryPersistence());
            client.setCallback(this);
            client.connect(connectOptions());
            client.subscribe(topic, 1);
            log.info("MQTT connected to {} and subscribed to {}", brokerUrl, topic);
        } catch (MqttException exception) {
            log.warn("MQTT connection failed. REST and WebSocket stay available. Cause: {}", exception.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException exception) {
            log.warn("MQTT disconnect failed: {}", exception.getMessage());
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe(topic, 1);
            log.info("MQTT subscription active on {}", topic);
        } catch (MqttException exception) {
            log.warn("MQTT resubscribe failed: {}", exception.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String sourceTopic, MqttMessage mqttMessage) {
        try {
            String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
            TelemetryMessage telemetryMessage = objectMapper.readValue(payload, TelemetryMessage.class);
            telemetryService.ingest(telemetryMessage, sourceTopic);
        } catch (Exception exception) {
            log.warn("Invalid MQTT payload on {}: {}", sourceTopic, exception.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Backend only subscribes.
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(connectionTimeoutSeconds);
        options.setKeepAliveInterval(keepAliveSeconds);
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            options.setPassword(password.toCharArray());
        }
        return options;
    }
}
