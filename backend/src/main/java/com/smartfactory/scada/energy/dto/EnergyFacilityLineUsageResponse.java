package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smartfactory.scada.facility.domain.FacilityStatus;
import com.smartfactory.scada.facility.domain.FacilityType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnergyFacilityLineUsageResponse {

	private Long facilityId;
	private String facilityName;
	private FacilityType facilityType;
	private FacilityStatus facilityStatus;
	private LocalDate usageDate;
	private BigDecimal todayUsageKwh;
	private BigDecimal yesterdayUsageKwh;
	private BigDecimal monthlyAverageKwh;
	private BigDecimal todayVsYesterdayRate;
	private BigDecimal todayVsMonthlyAverageRate;
	private LocalDateTime latestMeasuredAt;
}
