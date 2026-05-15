package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeakPowerFacilityRanking {

	private Long facilityId;
	private String facilityName;
	private BigDecimal usageKwh;
	private BigDecimal peakKw;
	private BigDecimal shareRate;
}
