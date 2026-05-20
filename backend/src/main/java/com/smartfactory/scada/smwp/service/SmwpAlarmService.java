package com.smartfactory.scada.smwp.service;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.plant.domain.Plant;
import com.smartfactory.scada.plant.mapper.PlantMapper;
import com.smartfactory.scada.smwp.dto.SmwpAlarmResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmwpAlarmService {

	private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("yy년M월d일HH:mm");
	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 200;

	private final PlantMapper plantMapper;
	private final AlarmMapper alarmMapper;
	private final Map<String, Plant> plantCache = new ConcurrentHashMap<>();

	public List<SmwpAlarmResponse> getAlarms(String plantName, Integer limit) {
		if (plantName == null || plantName.isBlank()) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		Plant plant = findPlantByName(plantName);

		return alarmMapper.findAlarms(plant.getId(), null, null, normalizeLimit(limit)).stream()
			.map(alarm -> new SmwpAlarmResponse(
				alarm.getOccurredAt() == null ? "-" : alarm.getOccurredAt().format(OCCURRED_AT_FORMATTER),
				alarm.getMessage()
			))
			.toList();
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}

		return Math.min(limit, MAX_LIMIT);
	}

	private Plant findPlantByName(String plantName) {
		String key = normalize(plantName);
		Plant cachedPlant = plantCache.get(key);
		if (cachedPlant != null) {
			return cachedPlant;
		}

		Plant plant = plantMapper.findAll().stream()
			.filter(candidate -> normalize(candidate.getName()).equals(key))
			.findFirst()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
		plantCache.put(key, plant);
		return plant;
	}

	private String normalize(String value) {
		return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
			.replaceAll("\\s+", "")
			.toLowerCase(Locale.ROOT);
	}
}
