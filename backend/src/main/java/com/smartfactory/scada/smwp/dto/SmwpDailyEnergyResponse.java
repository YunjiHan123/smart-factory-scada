package com.smartfactory.scada.smwp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SmwpDailyEnergyResponse(
	Long plantId,
	String plantName,
	LocalDate date,
	BigDecimal electricityKwh,
	BigDecimal gasM3,
	BigDecimal waterTon,
	BigDecimal solarKwh,
	LocalDateTime latestMeasuredAt
) {
}
