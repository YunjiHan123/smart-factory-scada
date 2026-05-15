package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PeakPowerMetricResponse(
	BigDecimal currentKw,
	BigDecimal peakUsageRate,
	BigDecimal intervalAverageKw,
	BigDecimal intervalMaxKw,
	BigDecimal thresholdKw,
	LocalDateTime measuredAt,
	LocalDateTime intervalAt
) {
}
