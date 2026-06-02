package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;
import com.smartfactory.scada.energy.domain.ElectricityTariffPlan;
import com.smartfactory.scada.energy.dto.ElectricityBillUsageBreakdownResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;

@Component
public class ElectricityBillCalculator {

	ElectricityBillCalculation calculate(
		ElectricityTariffPlan tariffPlan,
		ElectricityBillingDemandPeak demandPeak,
		List<ElectricityBillingUsageInterval> intervals
	) {
		BigDecimal billingDemandKw = demandPeak == null ? BigDecimal.ZERO : scalePower(demandPeak.getPeakKw());
		BigDecimal basicChargeKrw = money(billingDemandKw.multiply(tariffPlan.basicRateKrwPerKw()));
		List<ElectricityBillUsageBreakdownResponse> usageBreakdown = buildUsageBreakdown(intervals, tariffPlan);
		BigDecimal energyChargeKrw = usageBreakdown.stream()
			.map(ElectricityBillUsageBreakdownResponse::chargeKrw)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new ElectricityBillCalculation(
			billingDemandKw,
			demandPeak == null ? null : demandPeak.getMeasuredAt(),
			tariffPlan.basicRateKrwPerKw(),
			basicChargeKrw,
			money(energyChargeKrw),
			money(basicChargeKrw.add(energyChargeKrw)),
			usageBreakdown
		);
	}

	BigDecimal money(BigDecimal value) {
		return positive(value).setScale(0, RoundingMode.HALF_UP);
	}

	private List<ElectricityBillUsageBreakdownResponse> buildUsageBreakdown(
		List<ElectricityBillingUsageInterval> intervals,
		ElectricityTariffPlan tariffPlan
	) {
		Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> usageByPeriod = new EnumMap<>(ElectricitySeason.class);
		for (ElectricityBillingUsageInterval interval : intervals == null ? List.<ElectricityBillingUsageInterval>of() : intervals) {
			if (interval.getMeasuredAt() == null) {
				continue;
			}
			BigDecimal usageKwh = positive(interval.getUsageKwh());
			if (usageKwh.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			ElectricitySeason season = ElectricityBillingTimePolicy.seasonOf(interval.getMeasuredAt());
			ElectricityLoadPeriod loadPeriod = ElectricityBillingTimePolicy.loadPeriodOf(interval.getMeasuredAt());
			usageByPeriod.computeIfAbsent(season, ignored -> new EnumMap<>(ElectricityLoadPeriod.class))
				.merge(loadPeriod, usageKwh, BigDecimal::add);
		}

		List<ElectricityBillUsageBreakdownResponse> breakdown = new ArrayList<>();
		usageByPeriod.forEach((season, usageByLoadPeriod) -> usageByLoadPeriod.forEach((loadPeriod, usageKwh) -> {
			BigDecimal scaledUsage = scaleUsage(usageKwh);
			BigDecimal rate = tariffPlan.rateFor(season, loadPeriod);
			breakdown.add(new ElectricityBillUsageBreakdownResponse(
				season,
				season.displayName(),
				loadPeriod,
				loadPeriod.displayName(),
				scaledUsage,
				rate,
				money(scaledUsage.multiply(rate))
			));
		}));
		breakdown.sort(Comparator
			.comparing(ElectricityBillUsageBreakdownResponse::season)
			.thenComparing(ElectricityBillUsageBreakdownResponse::loadPeriod));
		return breakdown;
	}

	private BigDecimal positive(BigDecimal value) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			return BigDecimal.ZERO;
		}
		return value;
	}

	private BigDecimal scalePower(BigDecimal value) {
		return positive(value).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal scaleUsage(BigDecimal value) {
		return positive(value).setScale(2, RoundingMode.HALF_UP);
	}
}
