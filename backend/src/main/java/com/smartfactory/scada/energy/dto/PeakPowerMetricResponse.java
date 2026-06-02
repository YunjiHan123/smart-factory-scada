package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PeakPowerMetricResponse(
	BigDecimal currentKw,
	BigDecimal peakUsageRate,
	BigDecimal intervalAverageKw,
	BigDecimal intervalMaxKw,
	BigDecimal previousDayAverageKw,
	BigDecimal previousDayAverageRate,
	BigDecimal thresholdKw,
	LocalDateTime measuredAt,
	LocalDateTime intervalAt
) {
}
