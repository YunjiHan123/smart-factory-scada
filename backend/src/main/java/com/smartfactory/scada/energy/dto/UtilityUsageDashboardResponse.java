package com.smartfactory.scada.energy.dto;

import java.time.LocalDate;
import java.util.List;

import com.smartfactory.scada.energy.domain.UtilityUsagePeriod;

public record UtilityUsageDashboardResponse(
	Long plantId,
	LocalDate targetDate,
	UtilityUsagePeriod period,
	LocalDate periodFrom,
	LocalDate periodTo,
	UtilityUsageMetricResponse metrics,
	List<UtilityHourlyUsage> hourlyUsage,
	List<UtilityHourlyUsage> periodUsage,
	List<UtilityMeterStatus> meterStatuses,
	List<UtilityUsagePattern> patterns,
	List<UtilityUsagePlantComparison> plantComparison
) {
}
