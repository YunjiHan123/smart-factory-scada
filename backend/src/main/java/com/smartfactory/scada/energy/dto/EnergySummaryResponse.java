package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.SummaryType;

public record EnergySummaryResponse(
	Long id,
	Long plantId,
	Long facilityId,
	SummaryType summaryType,
	LocalDateTime summaryAt,
	BigDecimal electricityKwh,
	BigDecimal gasM3,
	BigDecimal waterTon,
	BigDecimal solarKwh,
	BigDecimal peakKw,
	BigDecimal carbonEmission
) {

	public static EnergySummaryResponse from(EnergySummary summary) {
		return new EnergySummaryResponse(
			summary.getId(),
			summary.getPlantId(),
			summary.getFacilityId(),
			summary.getSummaryType(),
			summary.getSummaryAt(),
			summary.getElectricityKwh(),
			summary.getGasM3(),
			summary.getWaterTon(),
			summary.getSolarKwh(),
			summary.getPeakKw(),
			summary.getCarbonEmission()
		);
	}
}
