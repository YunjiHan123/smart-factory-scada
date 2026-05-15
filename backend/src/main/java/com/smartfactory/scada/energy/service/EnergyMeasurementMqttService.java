package com.smartfactory.scada.energy.service;

import org.springframework.stereotype.Service;
import org.springframework.dao.TransientDataAccessException;

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

	private static final int MAX_SAVE_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF_MILLIS = 80L;

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

			saveMeasurementWithRetry(message);
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

	private void saveMeasurementWithRetry(EnergyMeasurementMessage message) {
		for (int attempt = 1; attempt <= MAX_SAVE_ATTEMPTS; attempt++) {
			try {
				energyService.saveMeasurement(message);
				return;
			}
			catch (TransientDataAccessException exception) {
				if (attempt == MAX_SAVE_ATTEMPTS) {
					throw exception;
				}

				log.warn(
					"Transient database error while saving MQTT energy message. Retrying. attempt={}/{}, plantId={}, facilityId={}, measuredAt={}, error={}",
					attempt,
					MAX_SAVE_ATTEMPTS,
					message.getPlantId(),
					message.getFacilityId(),
					message.getMeasuredAt(),
					exception.getClass().getSimpleName()
				);
				sleepBeforeRetry(attempt);
			}
		}
	}

	private void sleepBeforeRetry(int attempt) {
		try {
			Thread.sleep(RETRY_BACKOFF_MILLIS * attempt);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while retrying MQTT energy save.", exception);
		}
	}
}
