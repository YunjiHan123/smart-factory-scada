package com.smartfactory.scada.simulation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record SimulationRequest(
	@NotNull Long plantId,
	BigDecimal electricityReductionRate,
	BigDecimal gasReductionRate,
	BigDecimal waterReductionRate,
	BigDecimal peakReductionRate,
	BigDecimal solarIncreaseRate
) {
}
