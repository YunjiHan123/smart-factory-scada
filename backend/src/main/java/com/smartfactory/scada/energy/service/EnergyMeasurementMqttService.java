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
import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.energy.websocket.EnergyRealtimeWebSocketHandler;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyMeasurementMqttService {

	private static final long GENERATED_FACILITY_ID_OFFSET = 10_000L;
	private static final double[] LINE_EQUIPMENT_WEIGHTS = {0.22, 0.18, 0.16, 0.15, 0.14, 0.15};
	private static final int LINE_EQUIPMENT_COUNT = LINE_EQUIPMENT_WEIGHTS.length;
	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

	private final ObjectMapper objectMapper;
	private final EnergyMapper energyMapper;
	private final EnergyService energyService;
	private final EnergyRealtimeWebSocketHandler energyRealtimeWebSocketHandler;
	private final FacilityMapper facilityMapper;
	private final Map<String, LegacyMeasurementSnapshot> latestLegacyMeasurements = new ConcurrentHashMap<>();
	private final Map<String, EnergyMeasurementMessage> latestGeneratedMeasurements = new ConcurrentHashMap<>();
	private final Map<String, Boolean> facilityExistenceCache = new ConcurrentHashMap<>();
	private final Map<String, Boolean> generatedLineFacilitiesExistenceCache = new ConcurrentHashMap<>();

	public void handleMessage(String topic, String payload) {
		try {
			EnergyMeasurementMessage message = withTopicIds(
				topic,
				objectMapper.readValue(payload, EnergyMeasurementMessage.class)
			);
			log.info(
				"MQTT energy payload received. topic={}, plantId={}, facilityId={}, measuredAt={}, payload={}",
				topic,
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt(),
				payload
			);
			List<EnergyMeasurementMessage> normalizedMessages = normalizeLineMeasurement(message);

			for (EnergyMeasurementMessage normalizedMessage : normalizedMessages) {
				if (!facilityExists(normalizedMessage)) {
					log.warn(
						"MQTT energy message skipped because facility does not exist. topic={}, plantId={}, facilityId={}, measuredAt={}",
						topic,
						normalizedMessage.getPlantId(),
						normalizedMessage.getFacilityId(),
						normalizedMessage.getMeasuredAt()
					);
					continue;
				}

				long startedAt = System.currentTimeMillis();
				energyService.saveMeasurement(normalizedMessage);
				energyRealtimeWebSocketHandler.broadcast(normalizedMessage);
				log.info(
					"MQTT energy message persisted and broadcast. topic={}, plantId={}, facilityId={}, measuredAt={}, elapsedMs={}",
					topic,
					normalizedMessage.getPlantId(),
					normalizedMessage.getFacilityId(),
					normalizedMessage.getMeasuredAt(),
					System.currentTimeMillis() - startedAt
				);
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

	private EnergyMeasurementMessage withTopicIds(String topic, EnergyMeasurementMessage message) {
		if (message.getPlantId() != null && message.getFacilityId() != null) {
			return message;
		}

		String[] topicParts = topic == null ? new String[0] : topic.split("/");
		for (int index = 0; index < topicParts.length - 1; index += 1) {
			if (message.getPlantId() == null && "plant".equals(topicParts[index])) {
				message.setPlantId(parseLong(topicParts[index + 1]));
			}
			if (message.getFacilityId() == null && "facility".equals(topicParts[index])) {
				message.setFacilityId(parseLong(topicParts[index + 1]));
			}
		}
		return message;
	}

	private List<EnergyMeasurementMessage> normalizeLineMeasurement(EnergyMeasurementMessage message) {
		Long plantId = message.getPlantId();
		Long facilityId = message.getFacilityId();
		if (plantId == null || facilityId == null) {
			return List.of(message);
		}

		long expectedLinePrefix = plantId * 100L;
		long lineSequence = facilityId - expectedLinePrefix;
		if (lineSequence < 1 || lineSequence > 4) {
			return List.of(message);
		}

		Integer firstEquipmentSequence = switch ((int) lineSequence) {
			case 1 -> 1;
			case 2 -> 7;
			case 3 -> 19;
			case 4 -> 13;
			default -> null;
		};
		if (firstEquipmentSequence == null || !generatedLineFacilitiesExist(plantId, firstEquipmentSequence)) {
			return List.of(message);
		}

		String lineKey = measurementKey(plantId, facilityId);
		LegacyMeasurementSnapshot previousLine = latestLegacyMeasurements.put(
			lineKey,
			LegacyMeasurementSnapshot.from(message)
		);

		List<EnergyMeasurementMessage> generatedMessages = new ArrayList<>();
		for (int index = 0; index < LINE_EQUIPMENT_COUNT; index += 1) {
			long generatedFacilityId = (plantId * GENERATED_FACILITY_ID_OFFSET) + firstEquipmentSequence + index;
			generatedMessages.add(toGeneratedMeasurement(
				message,
				generatedFacilityId,
				distributionWeight(plantId, lineSequence, index),
				previousLine
			));
		}
		return generatedMessages;
	}

	private boolean generatedLineFacilitiesExist(Long plantId, int firstEquipmentSequence) {
		String cacheKey = plantId + ":" + firstEquipmentSequence;
		Boolean cached = generatedLineFacilitiesExistenceCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		for (int index = 0; index < LINE_EQUIPMENT_COUNT; index += 1) {
			long facilityId = (plantId * GENERATED_FACILITY_ID_OFFSET) + firstEquipmentSequence + index;
			if (!facilityExists(plantId, facilityId)) {
				generatedLineFacilitiesExistenceCache.put(cacheKey, false);
				return false;
			}
		}
		generatedLineFacilitiesExistenceCache.put(cacheKey, true);
		return true;
	}

	private EnergyMeasurementMessage toGeneratedMeasurement(
		EnergyMeasurementMessage source,
		Long generatedFacilityId,
		double weight,
		LegacyMeasurementSnapshot previousLine
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
		if (gasEstimated) {
			sourceGas = estimatedGasValue(sourceElectricity, source.getPlantId(), generatedFacilityId);
		}
		if (waterEstimated) {
			sourceWater = estimatedWaterValue(sourceElectricity, source.getPlantId(), generatedFacilityId);
		}
		if (solarEstimated) {
			sourceSolar = estimatedSolarValue(sourceElectricity, source.getMeasuredAt(), source.getPlantId(), generatedFacilityId);
		}
		Double previousSourceWater = previousLine == null ? null : previousLine.waterTon();
		Double previousSourceGas = previousLine == null ? null : previousLine.gasM3();
		if (gasEstimated && previousLine != null) {
			previousSourceGas = estimatedGasValue(previousLine.electricityKwh(), source.getPlantId(), generatedFacilityId);
		}
		if (waterEstimated && previousLine != null) {
			previousSourceWater = estimatedWaterValue(previousLine.electricityKwh(), source.getPlantId(), generatedFacilityId);
		}
		Double previousSourceSolar = previousLine == null ? null : previousLine.solarKwh();
		if (solarEstimated && previousLine != null) {
			previousSourceSolar = estimatedSolarValue(
				previousLine.electricityKwh(),
				previousLine.measuredAt(),
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
				previousLine == null ? null : previousLine.electricityKwh(),
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
		return energyMapper.findLatestMeasurement(plantId, facilityId)
			.map(this::toMessage)
			.orElse(null);
	}

	private EnergyMeasurementMessage toMessage(EnergyMeasurement measurement) {
		return new EnergyMeasurementMessage(
			measurement.getPlantId(),
			measurement.getFacilityId(),
			measurement.getMeasuredAt() == null ? null : measurement.getMeasuredAt().atZone(SERVICE_ZONE).toInstant(),
			valueOf(measurement.getElectricityKwh()),
			valueOf(measurement.getGasM3()),
			valueOf(measurement.getWaterTon()),
			valueOf(measurement.getSolarKwh()),
			valueOf(measurement.getPeakKw())
		);
	}

	private Long parseLong(String value) {
		try {
			return Long.valueOf(value);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private boolean facilityExists(EnergyMeasurementMessage message) {
		if (message.getFacilityId() == null) {
			return true;
		}
		if (message.getPlantId() == null) {
			return false;
		}
		return facilityExists(message.getPlantId(), message.getFacilityId());
	}

	private boolean facilityExists(Long plantId, Long facilityId) {
		String cacheKey = measurementKey(plantId, facilityId);
		return facilityExistenceCache.computeIfAbsent(cacheKey, key -> facilityMapper.findById(facilityId)
			.filter(facility -> plantId.equals(facility.getPlantId()))
			.isPresent());
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
		int hour = LocalTime.ofInstant(measuredAt, SERVICE_ZONE).getHour();
		if (hour < 7 || hour > 18) {
			return 0.0;
		}
		double daylightFactor = Math.sin(((hour - 6) / 13.0) * Math.PI);
		double plantFactor = 0.85 + (Math.floorMod(plantId == null ? 0 : plantId, 6) * 0.04);
		double facilityFactor = 0.7 + (Math.floorMod(facilityId == null ? 0 : facilityId, 7) * 0.03);
		return electricityKwh * 3.2 * daylightFactor * plantFactor * facilityFactor;
	}

	private double distributionWeight(Long plantId, long lineSequence, int equipmentIndex) {
		double plantFactor = 0.84 + ((Math.floorMod(plantId, 7)) * 0.035);
		double lineFactor = switch ((int) lineSequence) {
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
