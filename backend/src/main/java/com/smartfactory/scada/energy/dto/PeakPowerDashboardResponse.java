package com.smartfactory.scada.energy.dto;

import java.time.LocalDate;
import java.util.List;

import com.smartfactory.scada.energy.domain.PeakPowerPeriod;

public record PeakPowerDashboardResponse(
	Long plantId,
	LocalDate targetDate,
	PeakPowerPeriod period,
	LocalDate periodFrom,
	LocalDate periodTo,
	PeakPowerMetricResponse metrics,
	List<PeakPowerTrendPoint> trend,
	List<PeakPowerFacilityRanking> facilityRanking,
	List<PeakPowerHistory> history,
	List<PeakPowerPlantComparison> plantComparison
) {
}
