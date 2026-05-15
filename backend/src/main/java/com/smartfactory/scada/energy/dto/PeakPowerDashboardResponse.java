package com.smartfactory.scada.energy.dto;

import java.time.LocalDate;
import java.util.List;

public record PeakPowerDashboardResponse(
	Long plantId,
	LocalDate targetDate,
	PeakPowerMetricResponse metrics,
	List<PeakPowerTrendPoint> trend,
	List<PeakPowerFacilityRanking> facilityRanking,
	List<PeakPowerHistory> history
) {
}
