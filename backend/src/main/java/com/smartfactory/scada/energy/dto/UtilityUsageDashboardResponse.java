package com.smartfactory.scada.energy.dto;

import java.time.LocalDate;
import java.util.List;

public record UtilityUsageDashboardResponse(
	Long plantId,
	LocalDate targetDate,
	UtilityUsageMetricResponse metrics,
	List<UtilityHourlyUsage> hourlyUsage,
	List<UtilityMeterStatus> meterStatuses,
	List<UtilityUsagePattern> patterns
) {
}
