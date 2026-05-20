package com.smartfactory.scada.smwp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SmwpHourlyEnergyResponse(
	Long plantId,
	String plantName,
	LocalDate date,
	List<String> labels,
	List<BigDecimal> electricityKwh,
	List<BigDecimal> gasM3,
	List<BigDecimal> waterTon,
	List<BigDecimal> solarKwh,
	LocalDateTime latestMeasuredAt
) {
}
