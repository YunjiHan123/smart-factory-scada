package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

public record UtilityUsageMetricResponse(
	BigDecimal gasUsageM3,
	BigDecimal gasTotalM3,
	BigDecimal gasChangeRate,
	BigDecimal waterUsageTon,
	BigDecimal waterTotalTon,
	BigDecimal waterChangeRate
) {
}
