package com.smartfactory.scada.smwp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfactory.scada.energy.domain.EnergyType;

public record SmwpEnergyComparisonResponse(
	Long plantId,
	String plantName,
	LocalDate date,
	EnergyType energyType,
	String unit,
	BigDecimal previousMonthAverage,
	BigDecimal currentUsage,
	BigDecimal changeRate,
	String direction
) {
}
