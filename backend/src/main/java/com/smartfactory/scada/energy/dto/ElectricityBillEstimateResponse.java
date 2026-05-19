package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;

public record ElectricityBillEstimateResponse(
	Long plantId,
	LocalDate date,
	PeakPowerPeriod period,
	LocalDate periodFrom,
	LocalDate periodTo,
	ElectricityTariffCode tariffCode,
	String tariffName,
	String source,
	BigDecimal billingDemandKw,
	LocalDateTime billingPeakMeasuredAt,
	BigDecimal basicRateKrwPerKw,
	BigDecimal basicChargeKrw,
	BigDecimal energyChargeKrw,
	BigDecimal estimatedTotalKrw,
	String demandUnit,
	String usageUnit,
	String currencyUnit,
	List<ElectricityBillUsageBreakdownResponse> usageBreakdown,
	List<String> assumptions
) {
}
