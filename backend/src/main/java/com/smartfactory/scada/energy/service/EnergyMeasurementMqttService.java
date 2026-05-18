package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	private static final long GENERATED_FACILITY_ID_OFFSET = 10_000L;
	private static final double[] LINE_EQUIPMENT_WEIGHTS = {0.22, 0.18, 0.16, 0.15, 0.14, 0.15};
	private static final int LINE_EQUIPMENT_COUNT = LINE_EQUIPMENT_WEIGHTS.length;

	private final ObjectMapper objectMapper;
	private final EnergyService energyService;
	private final EnergyRealtimeWebSocketHandler energyRealtimeWebSocketHandler;
	private final Map<String, LegacyMeasurementSnapshot> latestLegacyMeasurements = new ConcurrentHashMap<>();
	private final Map<String, EnergyMeasurementMessage> latestGeneratedMeasurements = new ConcurrentHashMap<>();

	public void handleMessage(String topic, String payload) {
		try {
			EnergyMeasurementMessage message = objectMapper.readValue(payload, EnergyMeasurementMessage.class);
			List<EnergyMeasurementMessage> normalizedMessages = normalizeLegacyMeasurements(message);
			if (normalizedMessages.isEmpty()) {
				log.debug(
					"MQTT energy message skipped. topic={}, plantId={}, facilityId={}, measuredAt={}",
					topic,
					message.getPlantId(),
					message.getFacilityId(),
					message.getMeasuredAt()
				);
				return;
			}

			for (EnergyMeasurementMessage normalizedMessage : normalizedMessages) {
				log.info(
					"MQTT energy message received. topic={}, plantId={}, facilityId={}, measuredAt={}",
					topic,
					normalizedMessage.getPlantId(),
					normalizedMessage.getFacilityId(),
					normalizedMessage.getMeasuredAt()
				);

				energyService.saveMeasurement(normalizedMessage);
				energyRealtimeWebSocketHandler.broadcast(normalizedMessage);
			}
		}
		catch (JsonProcessingException exception) {
			log.error("Failed to parse MQTT energy payload. topic={}, payload={}", topic, payload, exception);
		}
		catch (Exception exception) {
			// Never let a single bad message stop the whole subscriber flow.
			log.error("Unexpected error while handling MQTT energy payload. topic={}", topic, exception);
		}
	}

	private List<EnergyMeasurementMessage> normalizeLegacyMeasurements(EnergyMeasurementMessage message) {
		Long plantId = message.getPlantId();
		Long facilityId = message.getFacilityId();
		if (plantId == null || facilityId == null) {
			return List.of(message);
		}

		long expectedLegacyPrefix = plantId * 100L;
		long legacySequence = facilityId - expectedLegacyPrefix;
		if (legacySequence < 1 || legacySequence > 5) {
			return List.of(message);
		}

		Integer firstLineSequence = switch ((int) legacySequence) {
			case 1 -> 1;  // Press line: F-001 ~ F-006
			case 2 -> 7;  // Body line: F-007 ~ F-012
			case 3 -> 19; // Paint line: F-019 ~ F-024
			case 4 -> 13; // Assembly line: F-013 ~ F-018
			default -> null;
		};
		if (firstLineSequence == null) {
			return List.of();
		}

		String legacyKey = measurementKey(plantId, facilityId);
		LegacyMeasurementSnapshot previousLegacy = latestLegacyMeasurements.put(
			legacyKey,
			LegacyMeasurementSnapshot.from(message)
		);

		List<EnergyMeasurementMessage> generatedMessages = new ArrayList<>();
		for (int index = 0; index < LINE_EQUIPMENT_COUNT; index += 1) {
			long generatedFacilityId = (plantId * GENERATED_FACILITY_ID_OFFSET) + firstLineSequence + index;
			generatedMessages.add(toGeneratedMeasurement(
				message,
				generatedFacilityId,
				distributionWeight(plantId, legacySequence, index),
				previousLegacy
			));
		}
		return generatedMessages;
	}

	private EnergyMeasurementMessage toGeneratedMeasurement(
		EnergyMeasurementMessage source,
		Long generatedFacilityId,
		double weight,
		LegacyMeasurementSnapshot previousLegacy
	) {
		String generatedKey = measurementKey(source.getPlantId(), generatedFacilityId);
		EnergyMeasurementMessage previousGenerated = latestGeneratedMeasurements.computeIfAbsent(
			generatedKey,
			key -> latestGeneratedMeasurement(source.getPlantId(), generatedFacilityId)
		);
		Double sourceElectricity = source.getElectricityKwh();
		Double sourceGas = source.getGasM3();
		Double sourceWater = source.getWaterTon();
		Double sourceSolar = source.getSolarKwh();
		boolean gasEstimated = sourceGas == null || sourceGas <= 0;
		boolean waterEstimated = sourceWater == null || sourceWater <= 0;
		boolean solarEstimated = sourceSolar == null || sourceSolar <= 0;
		if (sourceGas == null || sourceGas <= 0) {
			sourceGas = estimatedGasValue(sourceElectricity, source.getPlantId(), generatedFacilityId);
		}
		if (sourceWater == null || sourceWater <= 0) {
			sourceWater = estimatedWaterValue(sourceElectricity, source.getPlantId(), generatedFacilityId);
		}
		if (sourceSolar == null || sourceSolar <= 0) {
			sourceSolar = estimatedSolarValue(sourceElectricity, source.getMeasuredAt(), source.getPlantId(), generatedFacilityId);
		}
		Double previousSourceWater = previousLegacy == null ? null : previousLegacy.waterTon();
		Double previousSourceGas = previousLegacy == null ? null : previousLegacy.gasM3();
		if (gasEstimated && previousLegacy != null) {
			previousSourceGas = estimatedGasValue(previousLegacy.electricityKwh(), source.getPlantId(), generatedFacilityId);
		}
		if (waterEstimated && previousLegacy != null) {
			previousSourceWater = estimatedWaterValue(previousLegacy.electricityKwh(), source.getPlantId(), generatedFacilityId);
		}
		Double previousSourceSolar = previousLegacy == null ? null : previousLegacy.solarKwh();
		if (solarEstimated && previousLegacy != null) {
			previousSourceSolar = estimatedSolarValue(
				previousLegacy.electricityKwh(),
				previousLegacy.measuredAt(),
				source.getPlantId(),
				generatedFacilityId
			);
		}

		EnergyMeasurementMessage generated = new EnergyMeasurementMessage(
			source.getPlantId(),
			generatedFacilityId,
			source.getMeasuredAt(),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getElectricityKwh()),
				sourceElectricity,
				previousLegacy == null ? null : previousLegacy.electricityKwh(),
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getGasM3()),
				sourceGas,
				previousSourceGas,
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getWaterTon()),
				sourceWater,
				previousSourceWater,
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getSolarKwh()),
				sourceSolar,
				previousSourceSolar,
				weight
			),
			scaledValue(source.getPeakKw(), weight)
		);
		latestGeneratedMeasurements.put(generatedKey, generated);
		return generated;
	}

	private EnergyMeasurementMessage latestGeneratedMeasurement(Long plantId, Long facilityId) {
		return energyService.getLatestMeasurement(plantId, facilityId)
			.map(measurement -> new EnergyMeasurementMessage(
				measurement.plantId(),
				measurement.facilityId(),
				measurement.measuredAt() == null ? null : measurement.measuredAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
				valueOf(measurement.electricityKwh()),
				valueOf(measurement.gasM3()),
				valueOf(measurement.waterTon()),
				valueOf(measurement.solarKwh()),
				valueOf(measurement.peakKw())
			))
			.orElse(null);
	}

	private Double nextAccumulatedValue(Double previousGeneratedValue, Double sourceValue, Double previousSourceValue, double weight) {
		if (sourceValue == null) {
			return previousGeneratedValue;
		}

		if (previousSourceValue == null) {
			return previousGeneratedValue == null ? scaledValue(sourceValue, weight) : previousGeneratedValue;
		}
		if (previousGeneratedValue == null) {
			return scaledValue(sourceValue, weight);
		}

		double sourceDelta = sourceValue - previousSourceValue;
		if (sourceDelta < 0) {
			sourceDelta = 0;
		}
		return previousGeneratedValue + (sourceDelta * weight);
	}

	private Double scaledValue(Double value, double weight) {
		return value == null ? null : value * weight;
	}

	private Double estimatedGasValue(Double electricityKwh, Long plantId, Long facilityId) {
		if (electricityKwh == null) {
			return null;
		}
		double plantFactor = 0.94 + (Math.floorMod(plantId == null ? 0 : plantId, 6) * 0.045);
		double facilityFactor = 0.88 + (Math.floorMod(facilityId == null ? 0 : facilityId, 13) * 0.028);
		return electricityKwh * 2.55 * plantFactor * facilityFactor;
	}

	private Double estimatedWaterValue(Double electricityKwh, Long plantId, Long facilityId) {
		if (electricityKwh == null) {
			return null;
		}
		double plantFactor = 0.9 + (Math.floorMod(plantId == null ? 0 : plantId, 5) * 0.035);
		double facilityFactor = 0.82 + (Math.floorMod(facilityId == null ? 0 : facilityId, 11) * 0.018);
		return electricityKwh * 0.11 * plantFactor * facilityFactor;
	}

	private Double estimatedSolarValue(Double electricityKwh, java.time.Instant measuredAt, Long plantId, Long facilityId) {
		if (electricityKwh == null || measuredAt == null) {
			return null;
		}
		int hour = LocalTime.ofInstant(measuredAt, ZoneId.systemDefault()).getHour();
		if (hour < 7 || hour > 18) {
			return 0.0;
		}
		double daylightFactor = Math.sin(((hour - 6) / 13.0) * Math.PI);
		double plantFactor = 0.85 + (Math.floorMod(plantId == null ? 0 : plantId, 6) * 0.04);
		double facilityFactor = 0.7 + (Math.floorMod(facilityId == null ? 0 : facilityId, 7) * 0.03);
		return electricityKwh * 3.2 * daylightFactor * plantFactor * facilityFactor;
	}

	private double distributionWeight(Long plantId, long legacySequence, int equipmentIndex) {
		double plantFactor = 0.84 + ((Math.floorMod(plantId, 7)) * 0.035);
		double lineFactor = switch ((int) legacySequence) {
			case 1 -> 1.08;
			case 2 -> 0.96;
			case 3 -> 0.82;
			case 4 -> 1.15;
			default -> 1.0;
		};
		double equipmentFactor = 0.96 + (equipmentIndex * 0.025);
		return LINE_EQUIPMENT_WEIGHTS[equipmentIndex] * plantFactor * lineFactor * equipmentFactor;
	}

	private Double valueOf(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	private Double valueOf(Double value) {
		return value;
	}

	private String measurementKey(Long plantId, Long facilityId) {
		return plantId + ":" + facilityId;
	}

	private record LegacyMeasurementSnapshot(
		java.time.Instant measuredAt,
		Double electricityKwh,
		Double gasM3,
		Double waterTon,
		Double solarKwh
	) {

		private static LegacyMeasurementSnapshot from(EnergyMeasurementMessage message) {
			return new LegacyMeasurementSnapshot(
				message.getMeasuredAt(),
				message.getElectricityKwh(),
				message.getGasM3(),
				message.getWaterTon(),
				message.getSolarKwh()
			);
		}
	}
}
