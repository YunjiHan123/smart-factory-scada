package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

import com.smartfactory.scada.energy.domain.ElectricityTariffCode;

public record ElectricityBillComparisonRowResponse(
	ElectricityTariffCode tariffCode,
	String tariffName,
	BigDecimal basicRateKrwPerKw,
	BigDecimal billingDemandKw,
	BigDecimal basicChargeKrw,
	BigDecimal energyChargeKrw,
	BigDecimal estimatedTotalKrw,
	BigDecimal savingKrw,
	BigDecimal savingRate,
	int rank,
	boolean recommended
) {
}
