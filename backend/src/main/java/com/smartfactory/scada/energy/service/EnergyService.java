package com.smartfactory.scada.energy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.energy.domain.SummaryType;
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

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
