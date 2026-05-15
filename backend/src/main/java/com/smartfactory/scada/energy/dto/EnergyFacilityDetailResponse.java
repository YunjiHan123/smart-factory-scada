package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.facility.domain.FacilityStatus;
import com.smartfactory.scada.facility.domain.FacilityType;

public record EnergyFacilityDetailResponse(
	Long plantId,
	Long facilityId,
	String facilityName,
	FacilityType facilityType,
	FacilityStatus facilityStatus,
	EnergyType energyType,
	String unit,
	BigDecimal todayUsage,
	BigDecimal yesterdayUsage,
	BigDecimal changeAmount,
	BigDecimal changeRate,
	LocalDateTime latestMeasuredAt,
	List<EnergyUsagePointResponse> chart,
	List<EnergyUsageLogResponse> logs
) {

	public record EnergyUsagePointResponse(
		LocalDate date,
		LocalDateTime summaryAt,
		BigDecimal usage
	) {
	}

	public record EnergyUsageLogResponse(
		LocalDateTime measuredAt,
		BigDecimal usage,
		BigDecimal changeAmount,
		BigDecimal changeRate
	) {
	}
}
