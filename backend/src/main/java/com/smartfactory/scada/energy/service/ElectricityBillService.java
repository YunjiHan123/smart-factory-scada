package com.smartfactory.scada.energy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.ElectricityTariffPlan;
import com.smartfactory.scada.energy.domain.ElectricityTariffRegistry;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.ElectricityBillEstimateResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.energy.service.ElectricityBillingPeriodResolver.PeriodRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ElectricityBillService {

	private final EnergyMapper energyMapper;
	private final ElectricityBillCalculator calculator;

	@Transactional(readOnly = true)
	public ElectricityBillEstimateResponse estimate(
		Long plantId,
		LocalDate targetDate,
		PeakPowerPeriod period,
		ElectricityTariffCode tariffCode
	) {
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate resolvedDate = targetDate == null ? LocalDate.now() : targetDate;
		PeriodRange periodRange = ElectricityBillingPeriodResolver.resolve(resolvedDate, period, PeakPowerPeriod.DAY);
		LocalDateTime from = periodRange.fromInclusive().atStartOfDay();
		LocalDateTime to = periodRange.toExclusive().atStartOfDay();
		ElectricityTariffPlan tariffPlan = ElectricityTariffRegistry.find(tariffCode)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));

		ElectricityBillingDemandPeak demandPeak = energyMapper.findElectricityBillingDemandPeak(plantId, from, to)
			.orElse(null);
		List<ElectricityBillingUsageInterval> intervals =
			energyMapper.findElectricityBillingUsageIntervals(plantId, from, to);
		ElectricityBillCalculation calculation = calculator.calculate(tariffPlan, demandPeak, intervals);

		return new ElectricityBillEstimateResponse(
			plantId,
			resolvedDate,
			periodRange.period(),
			periodRange.fromInclusive(),
			periodRange.displayTo(),
			tariffPlan.code(),
			tariffPlan.name(),
			ElectricityTariffRegistry.SOURCE,
			calculation.billingDemandKw(),
			calculation.billingPeakMeasuredAt(),
			calculation.basicRateKrwPerKw(),
			calculation.basicChargeKrw(),
			calculation.energyChargeKrw(),
			calculation.estimatedTotalKrw(),
			ElectricityBillPolicy.DEMAND_UNIT,
			ElectricityBillPolicy.USAGE_UNIT,
			ElectricityBillPolicy.CURRENCY_UNIT,
			calculation.usageBreakdown(),
			ElectricityBillPolicy.ASSUMPTIONS
		);
	}
}
