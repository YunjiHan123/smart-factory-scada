package com.smartfactory.scada.simulation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartfactory.scada.simulation.domain.SimulationResult;

public record SimulationResponse(
	Long id,
	Long userId,
	Long plantId,
	String plantName,
	BigDecimal baseScore,
	String baseGrade,
	BigDecimal electricityReductionRate,
	BigDecimal gasReductionRate,
	BigDecimal waterReductionRate,
	BigDecimal peakReductionRate,
	BigDecimal solarIncreaseRate,
	BigDecimal expectedScore,
	String expectedGrade,
	String analysisResult,
	LocalDateTime createdAt,
	String key
) {

	public static SimulationResponse from(SimulationResult result) {
		return new SimulationResponse(
			result.getId(),
			result.getUserId(),
			result.getPlantId(),
			result.getPlantName(),
			result.getBaseScore(),
			result.getBaseGrade(),
			result.getElectricityReductionRate(),
			result.getGasReductionRate(),
			result.getWaterReductionRate(),
			result.getPeakReductionRate(),
			result.getSolarIncreaseRate(),
			result.getExpectedScore(),
			result.getExpectedGrade(),
			result.getAnalysisResult(),
			result.getCreatedAt(),
			result.getKey()
		);
	}
}
