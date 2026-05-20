package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;

public record ElectricityBillUsageBreakdownResponse(
	ElectricitySeason season,
	String seasonName,
	ElectricityLoadPeriod loadPeriod,
	String loadPeriodName,
	BigDecimal usageKwh,
	BigDecimal rateKrwPerKwh,
	BigDecimal chargeKrw
) {
}
