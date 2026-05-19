package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.smartfactory.scada.energy.domain.ElectricityBillDataStatus;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;

public record ElectricityBillComparisonResponse(
	Long plantId,
	LocalDate date,
	PeakPowerPeriod period,
	LocalDate periodFrom,
	LocalDate periodTo,
	ElectricityTariffCode baseTariffCode,
	String baseTariffName,
	ElectricityTariffCode bestTariffCode,
	String bestTariffName,
	BigDecimal billingDemandKw,
	LocalDateTime billingPeakMeasuredAt,
	BigDecimal baseEstimatedTotalKrw,
	BigDecimal bestEstimatedTotalKrw,
	BigDecimal estimatedSavingKrw,
	BigDecimal estimatedSavingRate,
	ElectricityBillDataStatus dataStatus,
	String source,
	String demandUnit,
	String usageUnit,
	String currencyUnit,
	List<ElectricityBillComparisonRowResponse> comparisons,
	List<String> assumptions
) {
}
