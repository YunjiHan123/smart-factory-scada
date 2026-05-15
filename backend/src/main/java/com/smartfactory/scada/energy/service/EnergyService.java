package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.dto.EnergyMeasurementResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnergyService {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 500;

	private final EnergyMapper energyMapper;

	@Transactional
	public void saveMeasurement(EnergyMeasurementMessage message) {
		EnergyMeasurement measurement = new EnergyMeasurement();
		measurement.setPlantId(message.getPlantId());
		measurement.setFacilityId(message.getFacilityId());
		measurement.setMeasuredAt(toLocalDateTime(message));
		measurement.setElectricityKwh(toBigDecimal(message.getElectricityKwh()));
		measurement.setGasM3(toBigDecimal(message.getGasM3()));
		measurement.setWaterTon(toBigDecimal(message.getWaterTon()));
		measurement.setSolarKwh(toBigDecimal(message.getSolarKwh()));
		measurement.setPeakKw(toBigDecimal(message.getPeakKw()));

		energyMapper.insertMeasurement(measurement);
	}

	@Transactional(readOnly = true)
	public List<EnergyMeasurementResponse> getMeasurements(
		Long plantId,
		Long facilityId,
		LocalDateTime from,
		LocalDateTime to,
		Integer limit
	) {
		int normalizedLimit = normalizeLimit(limit);
		return energyMapper.findMeasurements(plantId, facilityId, from, to, normalizedLimit)
			.stream()
			.map(EnergyMeasurementResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<EnergySummaryResponse> getSummaries(
		Long plantId,
		Long facilityId,
		SummaryType summaryType,
		LocalDateTime from,
		LocalDateTime to
	) {
		return energyMapper.findSummaries(plantId, facilityId, summaryType, from, to)
			.stream()
			.map(EnergySummaryResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public Optional<EnergyMeasurementResponse> getLatestMeasurement(Long plantId, Long facilityId) {
		return energyMapper.findLatestMeasurement(plantId, facilityId)
			.map(EnergyMeasurementResponse::from);
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private BigDecimal toBigDecimal(Double value) {
		return value == null ? null : BigDecimal.valueOf(value);
	}

	private LocalDateTime toLocalDateTime(EnergyMeasurementMessage message) {
		if (message.getMeasuredAt() == null) {
			return LocalDateTime.now();
		}
		return LocalDateTime.ofInstant(message.getMeasuredAt(), ZoneId.systemDefault());
	}
}
