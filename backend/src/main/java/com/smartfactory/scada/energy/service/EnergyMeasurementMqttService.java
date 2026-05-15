package com.smartfactory.scada.energy.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.websocket.EnergyRealtimeWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyMeasurementMqttService {

	private final ObjectMapper objectMapper;
	private final EnergyService energyService;
	private final EnergyRealtimeWebSocketHandler energyRealtimeWebSocketHandler;

	public void handleMessage(String topic, String payload) {
		try {
			EnergyMeasurementMessage message = objectMapper.readValue(payload, EnergyMeasurementMessage.class);

			log.info(
				"MQTT energy message received. topic={}, plantId={}, facilityId={}, measuredAt={}",
				topic,
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt()
			);

			energyService.saveMeasurement(message);
			energyRealtimeWebSocketHandler.broadcast(message);
		}
		catch (JsonProcessingException exception) {
			log.error("Failed to parse MQTT energy payload. topic={}, payload={}", topic, payload, exception);
		}
		catch (Exception exception) {
			// Never let a single bad message stop the whole subscriber flow.
			log.error("Unexpected error while handling MQTT energy payload. topic={}", topic, exception);
		}
	}
}
