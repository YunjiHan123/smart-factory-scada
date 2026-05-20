package com.smartfactory.scada.energy.domain;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public record ElectricityTariffPlan(
	ElectricityTariffCode code,
	String name,
	BigDecimal basicRateKrwPerKw,
	Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> energyRates
) {

	public ElectricityTariffPlan {
		energyRates = copyRates(energyRates);
	}

	public BigDecimal rateFor(ElectricitySeason season, ElectricityLoadPeriod loadPeriod) {
		return energyRates.getOrDefault(season, Map.of())
			.getOrDefault(loadPeriod, BigDecimal.ZERO);
	}

	private static Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> copyRates(
		Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> source
	) {
		EnumMap<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> copied = new EnumMap<>(ElectricitySeason.class);
		source.forEach((season, rates) -> copied.put(season, Map.copyOf(rates)));
		return Map.copyOf(copied);
	}
}
