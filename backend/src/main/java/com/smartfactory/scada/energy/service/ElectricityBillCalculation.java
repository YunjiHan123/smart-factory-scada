package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.smartfactory.scada.energy.dto.ElectricityBillUsageBreakdownResponse;

record ElectricityBillCalculation(
	BigDecimal billingDemandKw,
	LocalDateTime billingPeakMeasuredAt,
	BigDecimal basicRateKrwPerKw,
	BigDecimal basicChargeKrw,
	BigDecimal energyChargeKrw,
	BigDecimal estimatedTotalKrw,
	List<ElectricityBillUsageBreakdownResponse> usageBreakdown
) {
}
