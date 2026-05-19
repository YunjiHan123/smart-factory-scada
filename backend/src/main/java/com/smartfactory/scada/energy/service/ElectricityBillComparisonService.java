package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.ElectricityBillDataStatus;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.ElectricityTariffPlan;
import com.smartfactory.scada.energy.domain.ElectricityTariffRegistry;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonRowResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.energy.service.ElectricityBillingPeriodResolver.PeriodRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ElectricityBillComparisonService {

	private final EnergyMapper energyMapper;
	private final ElectricityBillCalculator calculator;

	@Transactional(readOnly = true)
	public ElectricityBillComparisonResponse compare(
		Long plantId,
		LocalDate targetDate,
		PeakPowerPeriod period,
		ElectricityTariffCode baseTariffCode
	) {
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate resolvedDate = targetDate == null ? LocalDate.now() : targetDate;
		PeriodRange periodRange = ElectricityBillingPeriodResolver.resolve(resolvedDate, period, PeakPowerPeriod.MONTH);
		LocalDateTime from = periodRange.fromInclusive().atStartOfDay();
		LocalDateTime to = periodRange.toExclusive().atStartOfDay();
		ElectricityTariffPlan basePlan = ElectricityTariffRegistry.find(baseTariffCode)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));

		ElectricityBillingDemandPeak demandPeak = energyMapper.findElectricityBillingDemandPeak(plantId, from, to)
			.orElse(null);
		List<ElectricityBillingUsageInterval> intervals =
			energyMapper.findElectricityBillingUsageIntervals(plantId, from, to);

		if (demandPeak == null && intervals.isEmpty()) {
			return emptyResponse(plantId, resolvedDate, periodRange, basePlan);
		}

		List<PlanCalculation> calculations = ElectricityTariffRegistry.allPlans()
			.stream()
			.map(plan -> new PlanCalculation(plan, calculator.calculate(plan, demandPeak, intervals)))
			.toList();
		PlanCalculation baseCalculation = calculations.stream()
			.filter(item -> item.plan().code() == basePlan.code())
			.findFirst()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
		List<PlanCalculation> ranked = calculations.stream()
			.sorted(Comparator
				.comparing((PlanCalculation item) -> item.calculation().estimatedTotalKrw())
				.thenComparing(item -> item.plan().code()))
			.toList();
		PlanCalculation bestCalculation = ranked.get(0);
		List<ElectricityBillComparisonRowResponse> rows = buildRows(baseCalculation, ranked);

		BigDecimal estimatedSavingKrw = saving(baseCalculation, bestCalculation);
		return new ElectricityBillComparisonResponse(
			plantId,
			resolvedDate,
			periodRange.period(),
			periodRange.fromInclusive(),
			periodRange.displayTo(),
			basePlan.code(),
			basePlan.name(),
			bestCalculation.plan().code(),
			bestCalculation.plan().name(),
			baseCalculation.calculation().billingDemandKw(),
			baseCalculation.calculation().billingPeakMeasuredAt(),
			baseCalculation.calculation().estimatedTotalKrw(),
			bestCalculation.calculation().estimatedTotalKrw(),
			estimatedSavingKrw,
			savingRate(estimatedSavingKrw, baseCalculation.calculation().estimatedTotalKrw()),
			ElectricityBillDataStatus.OK,
			ElectricityTariffRegistry.SOURCE,
			ElectricityBillPolicy.DEMAND_UNIT,
			ElectricityBillPolicy.USAGE_UNIT,
			ElectricityBillPolicy.CURRENCY_UNIT,
			rows,
			ElectricityBillPolicy.ASSUMPTIONS
		);
	}

	private ElectricityBillComparisonResponse emptyResponse(
		Long plantId,
		LocalDate resolvedDate,
		PeriodRange periodRange,
		ElectricityTariffPlan basePlan
	) {
		return new ElectricityBillComparisonResponse(
			plantId,
			resolvedDate,
			periodRange.period(),
			periodRange.fromInclusive(),
			periodRange.displayTo(),
			basePlan.code(),
			basePlan.name(),
			null,
			null,
			BigDecimal.ZERO,
			null,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			ElectricityBillDataStatus.EMPTY,
			ElectricityTariffRegistry.SOURCE,
			ElectricityBillPolicy.DEMAND_UNIT,
			ElectricityBillPolicy.USAGE_UNIT,
			ElectricityBillPolicy.CURRENCY_UNIT,
			List.of(),
			ElectricityBillPolicy.ASSUMPTIONS
		);
	}

	private List<ElectricityBillComparisonRowResponse> buildRows(
		PlanCalculation baseCalculation,
		List<PlanCalculation> ranked
	) {
		List<ElectricityBillComparisonRowResponse> rows = new ArrayList<>();
		BigDecimal baseTotal = baseCalculation.calculation().estimatedTotalKrw();
		for (int index = 0; index < ranked.size(); index++) {
			PlanCalculation item = ranked.get(index);
			BigDecimal savingKrw = saving(baseCalculation, item);
			rows.add(new ElectricityBillComparisonRowResponse(
				item.plan().code(),
				item.plan().name(),
				item.calculation().basicRateKrwPerKw(),
				item.calculation().billingDemandKw(),
				item.calculation().basicChargeKrw(),
				item.calculation().energyChargeKrw(),
				item.calculation().estimatedTotalKrw(),
				savingKrw,
				savingRate(savingKrw, baseTotal),
				index + 1,
				index == 0
			));
		}
		return rows;
	}

	private BigDecimal saving(PlanCalculation baseCalculation, PlanCalculation comparedCalculation) {
		return baseCalculation.calculation().estimatedTotalKrw()
			.subtract(comparedCalculation.calculation().estimatedTotalKrw())
			.setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal savingRate(BigDecimal savingKrw, BigDecimal baseTotalKrw) {
		if (baseTotalKrw == null || baseTotalKrw.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return savingKrw.multiply(BigDecimal.valueOf(100))
			.divide(baseTotalKrw, 1, RoundingMode.HALF_UP);
	}

	private record PlanCalculation(ElectricityTariffPlan plan, ElectricityBillCalculation calculation) {
	}
}
