package com.smartfactory.scada.energy.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnergySummary {

	private Long id;
	private Long plantId;
	private Long facilityId;
	private SummaryType summaryType;
	private LocalDateTime summaryAt;
	private BigDecimal electricityKwh;
	private BigDecimal gasM3;
	private BigDecimal waterTon;
	private BigDecimal solarKwh;
	private BigDecimal peakKw;
	private BigDecimal carbonEmission;
}
