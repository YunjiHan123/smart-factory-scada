package com.smartfactory.scada.esg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EsgEnvironmentDashboardResponse(
	LocalDate from,
	LocalDate to,
	Long selectedPlantId,
	List<EsgEnvironmentPlantResponse> plants,
	EsgEnvironmentPlantResponse selectedPlant,
	List<EsgEnvironmentMetricResponse> metrics,
	List<EsgEnvironmentAlertResponse> alerts,
	EsgEnvironmentLogicResponse logic
) {

	public record EsgEnvironmentPlantResponse(
		int rank,
		Long plantId,
		String plantName,
		BigDecimal latitude,
		BigDecimal longitude,
		BigDecimal totalScore,
		String grade,
		BigDecimal changeRate,
		BigDecimal waterChangeRate,
		BigDecimal solarChangeRate,
		BigDecimal carbonEmission,
		BigDecimal waterUsage,
		BigDecimal solarGeneration,
		BigDecimal currentPeakKw,
		BigDecimal intervalMaxPeakKw,
		BigDecimal peakReductionRate,
		BigDecimal electricityKwh,
		BigDecimal gasM3,
		BigDecimal waterScore,
		BigDecimal solarScore,
		BigDecimal peakScore,
		BigDecimal carbonScore,
		BigDecimal electricityScore,
		BigDecimal gasScore,
		Integer facilityCount,
		Integer measurementCount,
		LocalDateTime latestMeasuredAt
	) {
	}

	public record EsgEnvironmentMetricResponse(
		String key,
		String label,
		BigDecimal score,
		String unit,
		BigDecimal value,
		BigDecimal changeRate,
		String sourceLabel
	) {
	}

	public record EsgEnvironmentAlertResponse(
		String level,
		String title,
		String message,
		Long plantId,
		String plantName
	) {
	}

	public record EsgEnvironmentLogicResponse(
		String normalization,
		String weight,
		String gradeMapping
	) {
	}
}
