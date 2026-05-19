package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.ElectricityTariffPlan;
import com.smartfactory.scada.energy.domain.ElectricityTariffRegistry;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.ElectricityBillEstimateResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillUsageBreakdownResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;
import com.smartfactory.scada.energy.mapper.EnergyMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ElectricityBillService {

	private static final String DEMAND_UNIT = "kW";
	private static final String USAGE_UNIT = "kWh";
	private static final String CURRENCY_UNIT = "KRW";
	private static final List<String> ASSUMPTIONS = List.of(
		"피크 화면 의사결정용 추정 전기요금입니다.",
		"기본요금은 선택 기간 내 15분 최대 피크전력을 기준으로 산정합니다.",
		"부가가치세, 전력산업기반기금, 기후환경요금, 연료비조정액, 역률요금, 휴일/토요일 할인은 제외합니다.",
		"요금 단가는 사용자 첨부 한전 산업용전력(을) 표를 v1 기준 데이터로 사용합니다."
	);

	private final EnergyMapper energyMapper;

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
		PeakPowerPeriodRange periodRange = resolvePeriodRange(resolvedDate, period);
		LocalDateTime from = periodRange.from().atStartOfDay();
		LocalDateTime to = periodRange.to().atStartOfDay();
		ElectricityTariffPlan tariffPlan = ElectricityTariffRegistry.find(tariffCode)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));

		ElectricityBillingDemandPeak demandPeak = energyMapper.findElectricityBillingDemandPeak(plantId, from, to)
			.orElse(null);
		List<ElectricityBillingUsageInterval> intervals =
			energyMapper.findElectricityBillingUsageIntervals(plantId, from, to);

		BigDecimal billingDemandKw = demandPeak == null ? BigDecimal.ZERO : scalePower(demandPeak.getPeakKw());
		BigDecimal basicChargeKrw = money(billingDemandKw.multiply(tariffPlan.basicRateKrwPerKw()));
		List<ElectricityBillUsageBreakdownResponse> usageBreakdown = buildUsageBreakdown(intervals, tariffPlan);
		BigDecimal energyChargeKrw = usageBreakdown.stream()
			.map(ElectricityBillUsageBreakdownResponse::chargeKrw)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new ElectricityBillEstimateResponse(
			plantId,
			resolvedDate,
			periodRange.period(),
			periodRange.from(),
			periodRange.to().minusDays(1),
			tariffPlan.code(),
			tariffPlan.name(),
			ElectricityTariffRegistry.SOURCE,
			billingDemandKw,
			demandPeak == null ? null : demandPeak.getMeasuredAt(),
			tariffPlan.basicRateKrwPerKw(),
			basicChargeKrw,
			money(energyChargeKrw),
			money(basicChargeKrw.add(energyChargeKrw)),
			DEMAND_UNIT,
			USAGE_UNIT,
			CURRENCY_UNIT,
			usageBreakdown,
			ASSUMPTIONS
		);
	}

	private List<ElectricityBillUsageBreakdownResponse> buildUsageBreakdown(
		List<ElectricityBillingUsageInterval> intervals,
		ElectricityTariffPlan tariffPlan
	) {
		Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> usageByPeriod = new EnumMap<>(ElectricitySeason.class);
		for (ElectricityBillingUsageInterval interval : intervals) {
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

	private PeakPowerPeriodRange resolvePeriodRange(LocalDate targetDate, PeakPowerPeriod period) {
		PeakPowerPeriod resolvedPeriod = period == null ? PeakPowerPeriod.DAY : period;
		return switch (resolvedPeriod) {
			case DAY -> new PeakPowerPeriodRange(resolvedPeriod, targetDate, targetDate.plusDays(1));
			case WEEK -> {
				LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				yield new PeakPowerPeriodRange(resolvedPeriod, weekStart, weekStart.plusWeeks(1));
			}
			case MONTH -> {
				LocalDate monthStart = targetDate.withDayOfMonth(1);
				yield new PeakPowerPeriodRange(resolvedPeriod, monthStart, monthStart.plusMonths(1));
			}
		};
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

	private BigDecimal money(BigDecimal value) {
		return positive(value).setScale(0, RoundingMode.HALF_UP);
	}

	private record PeakPowerPeriodRange(PeakPowerPeriod period, LocalDate from, LocalDate to) {
	}
}
