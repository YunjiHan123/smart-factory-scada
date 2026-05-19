package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
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

	private final ObjectMapper objectMapper;
	private final EnergyService energyService;
	private final EnergyRealtimeWebSocketHandler energyRealtimeWebSocketHandler;
	private final FacilityMapper facilityMapper;
	private final Map<String, LegacyMeasurementSnapshot> latestLegacyMeasurements = new ConcurrentHashMap<>();
	private final Map<String, EnergyMeasurementMessage> latestGeneratedMeasurements = new ConcurrentHashMap<>();

	public void handleMessage(String topic, String payload) {
		try {
			EnergyMeasurementMessage message = withTopicIds(
				topic,
				objectMapper.readValue(payload, EnergyMeasurementMessage.class)
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
		for (int index = 0; index < LINE_EQUIPMENT_COUNT; index += 1) {
			long facilityId = (plantId * GENERATED_FACILITY_ID_OFFSET) + firstEquipmentSequence + index;
			if (facilityMapper.findById(facilityId)
				.filter(facility -> plantId.equals(facility.getPlantId()))
				.isEmpty()) {
				return false;
			}
		}
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

		EnergyMeasurementMessage generated = new EnergyMeasurementMessage(
			source.getPlantId(),
			generatedFacilityId,
			source.getMeasuredAt(),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getElectricityKwh()),
				source.getElectricityKwh(),
				previousLine == null ? null : previousLine.electricityKwh(),
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getGasM3()),
				source.getGasM3(),
				previousLine == null ? null : previousLine.gasM3(),
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getWaterTon()),
				source.getWaterTon(),
				previousLine == null ? null : previousLine.waterTon(),
				weight
			),
			nextAccumulatedValue(
				valueOf(previousGenerated == null ? null : previousGenerated.getSolarKwh()),
				source.getSolarKwh(),
				previousLine == null ? null : previousLine.solarKwh(),
				weight
			),
			scaledValue(source.getPeakKw(), weight)
		);
		latestGeneratedMeasurements.put(generatedKey, generated);
		return generated;
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
		return facilityMapper.findById(message.getFacilityId())
			.filter(facility -> message.getPlantId().equals(facility.getPlantId()))
			.isPresent();
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
		Double electricityKwh,
		Double gasM3,
		Double waterTon,
		Double solarKwh
	) {

		private static LegacyMeasurementSnapshot from(EnergyMeasurementMessage message) {
			return new LegacyMeasurementSnapshot(
				message.getElectricityKwh(),
				message.getGasM3(),
				message.getWaterTon(),
				message.getSolarKwh()
			);
		}
	}
}
