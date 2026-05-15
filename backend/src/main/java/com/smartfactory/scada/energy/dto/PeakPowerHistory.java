package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeakPowerHistory {

	private LocalDateTime measuredAt;
	private BigDecimal peakKw;
	private BigDecimal peakUsageRate;
	private Integer durationMinutes;
	private BigDecimal thresholdKw;
	private Boolean exceeded;
}
