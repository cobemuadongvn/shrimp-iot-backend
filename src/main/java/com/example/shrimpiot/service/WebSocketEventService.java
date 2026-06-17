package com.example.shrimpiot.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.shrimpiot.dto.AlertResponse;
import com.example.shrimpiot.dto.NotificationResponse;
import com.example.shrimpiot.dto.RelayStateResponse;
import com.example.shrimpiot.dto.SensorReadingResponse;

@Service
public class WebSocketEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishSensorReading(SensorReadingResponse reading) {
        if (reading == null) return;
        String dest = "/topic/device/" + reading.getDeviceId() + "/readings";
        messagingTemplate.convertAndSend(dest, reading);
    }

    public void publishRelayState(RelayStateResponse relayState) {
        if (relayState == null) return;
        String dest = "/topic/device/" + relayState.getDeviceId() + "/relays";
        messagingTemplate.convertAndSend(dest, relayState);
    }

    public void publishAlert(AlertResponse alert) {
        if (alert == null) return;
        String dest = "/topic/device/" + alert.getDeviceId() + "/alerts";
        messagingTemplate.convertAndSend(dest, alert);
    }

    public void publishInAppNotification(NotificationResponse notification) {
        if (notification == null) return;
        String deviceDest = "/topic/device/" + notification.getDeviceId() + "/notifications";
        messagingTemplate.convertAndSend(deviceDest, notification);

        if (notification.getRecipientUserId() != null) {
            String userDest = "/topic/user/" + notification.getRecipientUserId() + "/notifications";
            messagingTemplate.convertAndSend(userDest, notification);
        }
    }
}

