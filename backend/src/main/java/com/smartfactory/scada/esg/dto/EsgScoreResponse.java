package com.smartfactory.scada.esg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfactory.scada.esg.domain.EsgGrade;
import com.smartfactory.scada.esg.domain.EsgScore;

public record EsgScoreResponse(
	Long id,
	Long plantId,
	String plantName,
	LocalDate targetMonth,
	BigDecimal electricityScore,
	BigDecimal gasScore,
	BigDecimal waterScore,
	BigDecimal solarScore,
	BigDecimal peakScore,
	BigDecimal carbonScore,
	BigDecimal totalScore,
	EsgGrade grade
) {

	public static EsgScoreResponse from(EsgScore score) {
		return new EsgScoreResponse(
			score.getId(),
			score.getPlantId(),
			score.getPlantName(),
			score.getTargetMonth(),
			score.getElectricityScore(),
			score.getGasScore(),
			score.getWaterScore(),
			score.getSolarScore(),
			score.getPeakScore(),
			score.getCarbonScore(),
			score.getTotalScore(),
			score.getGrade()
		);
	}
}
