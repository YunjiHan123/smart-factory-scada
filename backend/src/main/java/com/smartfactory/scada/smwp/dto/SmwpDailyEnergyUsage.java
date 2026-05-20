package com.smartfactory.scada.smwp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmwpDailyEnergyUsage {

	private Long plantId;
	private BigDecimal electricityKwh;
	private BigDecimal gasM3;
	private BigDecimal waterTon;
	private BigDecimal solarKwh;
	private LocalDateTime latestMeasuredAt;
}
