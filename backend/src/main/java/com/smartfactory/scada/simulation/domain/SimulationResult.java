package com.smartfactory.scada.simulation.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationResult {

	private Long id;
	private Long userId;
	private Long plantId;
	private String plantName;
	private BigDecimal baseScore;
	private String baseGrade;
	private BigDecimal electricityReductionRate;
	private BigDecimal gasReductionRate;
	private BigDecimal waterReductionRate;
	private BigDecimal peakReductionRate;
	private BigDecimal solarIncreaseRate;
	private BigDecimal expectedScore;
	private String expectedGrade;
	private String analysisResult;
	private LocalDateTime createdAt;
}
