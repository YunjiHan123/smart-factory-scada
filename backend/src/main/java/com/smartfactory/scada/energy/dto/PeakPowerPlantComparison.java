package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeakPowerPlantComparison {

	private Long plantId;
	private String plantName;
	private BigDecimal periodPeakKw;
	private BigDecimal periodAverageKw;
	private BigDecimal thresholdKw;
	private BigDecimal peakUsageRate;
	private Boolean exceeded;
	private Integer rank;
}
