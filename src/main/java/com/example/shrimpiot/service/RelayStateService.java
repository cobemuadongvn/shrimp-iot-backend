package com.example.shrimpiot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.shrimpiot.dto.RelayStateResponse;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.model.RelayState;
import com.example.shrimpiot.repository.RelayStateRepository;

@Service
public class RelayStateService {
    private final RelayStateRepository repository;
    private final WebSocketEventService webSocketEventService;

    public RelayStateService(RelayStateRepository repository, WebSocketEventService webSocketEventService) {
        this.repository = repository;
        this.webSocketEventService = webSocketEventService;
    }

    /**
     * Lấy trạng thái hiện tại của relay
     */
    public RelayStateResponse getRelayState(String deviceId, Integer relayNo) {
        RelayState state = repository.findByDeviceIdAndRelayNo(deviceId, relayNo)
                .orElseThrow(() -> new IllegalArgumentException("Relay state not found: device=" + deviceId + ", relay=" + relayNo));
        return new RelayStateResponse(state);
    }

    /**
     * Lấy danh sách trạng thái tất cả relay của thiết bị
     */
    public List<RelayStateResponse> getRelayStatesByDevice(String deviceId) {
        return repository.findByDeviceId(deviceId).stream()
                .map(RelayStateResponse::new)
                .toList();
    }

    /**
     * Tạo hoặc lấy relay state (nếu chưa có thì tạo mới)
     */
    public RelayState getOrCreateRelayState(String deviceId, Integer relayNo, String relayName) {
        return repository.findByDeviceIdAndRelayNo(deviceId, relayNo)
                .orElseGet(() -> {
                    RelayState newState = new RelayState(deviceId, relayNo, relayName);
                    return repository.save(newState);
                });
    }

    /**
     * Cập nhật trạng thái relay khi nhận được lệnh ACK từ device
     */
    public RelayStateResponse updateRelayState(String deviceId, Integer relayNo, RelayAction action, Long commandId) {
        RelayState state = repository.findByDeviceIdAndRelayNo(deviceId, relayNo)
                .orElseThrow(() -> new IllegalArgumentException("Relay state not found: device=" + deviceId + ", relay=" + relayNo));
        
        state.setCurrentState(action);
        state.setLastCommandId(commandId);
        state.setLastUpdatedAt(LocalDateTime.now());
        RelayState updated = repository.save(state);
        RelayStateResponse resp = new RelayStateResponse(updated);
        try { if (webSocketEventService != null) webSocketEventService.publishRelayState(resp); } catch (Exception ignored) {}
        return resp;
    }

    /**
     * Cập nhật từ DeviceCommand (gọi sau khi command được ACK)
     */
    public RelayStateResponse updateFromCommand(DeviceCommand command) {
        return updateRelayState(
                command.getDeviceId(),
                command.getRelayNo(),
                command.getAction(),
                command.getId()
        );
    }

    /**
     * Kiểm tra relay hiện đang ON hay OFF
     */
    public boolean isRelayOn(String deviceId, Integer relayNo) {
        return repository.findByDeviceIdAndRelayNo(deviceId, relayNo)
                .map(state -> state.getCurrentState() == RelayAction.ON)
                .orElse(false);
    }

    /**
     * Kiểm tra relay hiện đang OFF
     */
    public boolean isRelayOff(String deviceId, Integer relayNo) {
        return repository.findByDeviceIdAndRelayNo(deviceId, relayNo)
                .map(state -> state.getCurrentState() == RelayAction.OFF)
                .orElse(true);
    }

    /**
     * Khởi tạo relay states cho một device (gọi khi device được đăng ký)
     */
    public void initializeRelayStatesForDevice(String deviceId, List<com.example.shrimpiot.model.DeviceRelay> relays) {
        for (com.example.shrimpiot.model.DeviceRelay relay : relays) {
            getOrCreateRelayState(deviceId, relay.getRelayNo(), relay.getName());
        }
    }

    /**
     * Xóa tất cả relay states của device
     */
    public void deleteRelayStatesByDevice(String deviceId) {
        repository.deleteByDeviceId(deviceId);
    }
}
