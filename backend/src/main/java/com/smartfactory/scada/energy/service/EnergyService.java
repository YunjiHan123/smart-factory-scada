package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.alarm.domain.Alarm;
import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.domain.AlarmType;
import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse.EnergyUsageLogResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse.EnergyUsagePointResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityLineUsageResponse;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.dto.EnergyMeasurementResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.energy.dto.PeakPowerDashboardResponse;
import com.smartfactory.scada.energy.dto.PeakPowerFacilityRanking;
import com.smartfactory.scada.energy.dto.PeakPowerHistory;
import com.smartfactory.scada.energy.dto.PeakPowerMetricResponse;
import com.smartfactory.scada.energy.dto.PeakPowerPlantComparison;
import com.smartfactory.scada.energy.dto.PeakPowerTrendPoint;
import com.smartfactory.scada.energy.dto.UtilityHourlyUsage;
import com.smartfactory.scada.energy.dto.UtilityUsageDashboardResponse;
import com.smartfactory.scada.energy.dto.UtilityUsageMetricResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.facility.domain.Facility;
import com.smartfactory.scada.facility.domain.FacilityType;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnergyService {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 500;
	private static final BigDecimal FACILITY_PEAK_THRESHOLD_KW = BigDecimal.valueOf(1400);
	private static final BigDecimal ELECTRICITY_WARNING_DELTA = BigDecimal.valueOf(80);
	private static final BigDecimal ELECTRICITY_CRITICAL_DELTA = BigDecimal.valueOf(130);
	private static final BigDecimal GAS_WARNING_DELTA = BigDecimal.valueOf(8);
	private static final BigDecimal GAS_CRITICAL_DELTA = BigDecimal.valueOf(15);
	private static final BigDecimal WATER_WARNING_DELTA = BigDecimal.valueOf(1.5);
	private static final BigDecimal WATER_CRITICAL_DELTA = BigDecimal.valueOf(3);

	private final EnergyMapper energyMapper;
	private final FacilityMapper facilityMapper;
	private final AlarmMapper alarmMapper;

	@Transactional
	public void saveMeasurement(EnergyMeasurementMessage message) {
		EnergyMeasurement measurement = new EnergyMeasurement();
		measurement.setPlantId(message.getPlantId());
		measurement.setFacilityId(message.getFacilityId());
		measurement.setMeasuredAt(toLocalDateTime(message));
		measurement.setElectricityKwh(toBigDecimal(message.getElectricityKwh()));
		measurement.setGasM3(toBigDecimal(message.getGasM3()));
		measurement.setWaterTon(toBigDecimal(message.getWaterTon()));
		measurement.setSolarKwh(toBigDecimal(message.getSolarKwh()));
		measurement.setPeakKw(toBigDecimal(message.getPeakKw()));

		EnergyMeasurement previousMeasurement = energyMapper.findPreviousMeasurement(
				measurement.getPlantId(),
				measurement.getFacilityId(),
				measurement.getMeasuredAt()
			)
			.orElse(null);

		energyMapper.insertMeasurement(measurement);
		createRealtimeAlarms(measurement, previousMeasurement);
	}

	@Transactional(readOnly = true)
	public List<EnergyMeasurementResponse> getMeasurements(
		Long plantId,
		Long facilityId,
		LocalDateTime from,
		LocalDateTime to,
		Integer limit
	) {
		int normalizedLimit = normalizeLimit(limit);
		return energyMapper.findMeasurements(plantId, facilityId, from, to, normalizedLimit)
			.stream()
			.map(EnergyMeasurementResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<EnergySummaryResponse> getSummaries(
		Long plantId,
		Long facilityId,
		SummaryType summaryType,
		LocalDateTime from,
		LocalDateTime to
	) {
		return energyMapper.findSummaries(plantId, facilityId, summaryType, from, to)
			.stream()
			.map(EnergySummaryResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public Optional<EnergyMeasurementResponse> getLatestMeasurement(Long plantId, Long facilityId) {
		return energyMapper.findLatestMeasurement(plantId, facilityId)
			.map(EnergyMeasurementResponse::from);
	}

	@Transactional(readOnly = true)
	public PeakPowerDashboardResponse getPeakDashboard(Long plantId, LocalDate targetDate, PeakPowerPeriod period) {
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate resolvedDate = targetDate == null ? LocalDate.now() : targetDate;
		PeakPowerPeriodRange periodRange = resolvePeakPowerPeriodRange(resolvedDate, period);
		PeakPowerPeriodRange previousPeriodRange = previousPeakPowerPeriodRange(periodRange);
		LocalDateTime from = periodRange.from().atStartOfDay();
		LocalDateTime to = periodRange.to().atStartOfDay();
		BigDecimal thresholdKw = peakThresholdForPlant(plantId);

		List<PeakPowerTrendPoint> trend = findPeakTrend(plantId, from, to, periodRange.period());
		List<PeakPowerTrendPoint> previousPeriodTrend = findPeakTrend(
			plantId,
			previousPeriodRange.from().atStartOfDay(),
			previousPeriodRange.to().atStartOfDay(),
			periodRange.period()
		);
		List<PeakPowerFacilityRanking> ranking = energyMapper.findPeakPowerFacilityRanking(plantId, from, to, 5);
		List<PeakPowerHistory> history = energyMapper.findPeakPowerHistory(
			plantId,
			from,
			to,
			thresholdKw,
			periodRange.period() == PeakPowerPeriod.DAY ? 10 : 20
		);
		List<PeakPowerPlantComparison> plantComparison = energyMapper.findPeakPowerPlantComparison(
			from,
			to,
			FACILITY_PEAK_THRESHOLD_KW
		);

		EnergyMeasurement latestMeasurement = energyMapper.findLatestPlantMeasurement(plantId, from, to).orElse(null);
		PeakPowerTrendPoint latestInterval = trend.isEmpty() ? null : trend.get(trend.size() - 1);
		BigDecimal currentKw = latestMeasurement == null ? BigDecimal.ZERO : zeroIfNull(latestMeasurement.getPeakKw());
		BigDecimal intervalAverageKw = latestInterval == null ? BigDecimal.ZERO : zeroIfNull(latestInterval.getAverageKw());
		BigDecimal intervalMaxKw = latestInterval == null ? BigDecimal.ZERO : zeroIfNull(latestInterval.getMaxKw());
		BigDecimal previousPeriodAverageKw = averagePeak(previousPeriodTrend);

		PeakPowerMetricResponse metrics = new PeakPowerMetricResponse(
			currentKw,
			rateOf(currentKw, thresholdKw),
			intervalAverageKw,
			intervalMaxKw,
			previousPeriodAverageKw,
			rateOf(currentKw, previousPeriodAverageKw),
			thresholdKw,
			latestMeasurement == null ? null : latestMeasurement.getMeasuredAt(),
			latestInterval == null ? null : latestInterval.getMeasuredAt()
		);

		return new PeakPowerDashboardResponse(
			plantId,
			resolvedDate,
			periodRange.period(),
			periodRange.from(),
			periodRange.to().minusDays(1),
			metrics,
			trend,
			ranking,
			history,
			plantComparison
		);
	}

	@Transactional(readOnly = true)
	public UtilityUsageDashboardResponse getUtilityDashboard(Long plantId, LocalDate targetDate) {
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate resolvedDate = targetDate == null ? LocalDate.now() : targetDate;
		LocalDateTime from = resolvedDate.atStartOfDay();
		LocalDateTime to = resolvedDate.plusDays(1).atStartOfDay();
		LocalDateTime yesterdayFrom = resolvedDate.minusDays(1).atStartOfDay();
		LocalDateTime patternFrom = resolvedDate.minusDays(6).atStartOfDay();
		LocalDateTime monthFrom = resolvedDate.withDayOfMonth(1).atStartOfDay();

		List<UtilityHourlyUsage> hourlyUsage = energyMapper.findUtilityHourlyUsage(plantId, from, to);
		List<UtilityHourlyUsage> yesterdayHourlyUsage = energyMapper.findUtilityHourlyUsage(plantId, yesterdayFrom, from);
		UtilityHourlyUsage monthlyUsage = energyMapper.findUtilityUsageTotal(plantId, monthFrom, to).orElse(null);
		BigDecimal gasUsage = sumGasUsage(hourlyUsage);
		BigDecimal waterUsage = sumWaterUsage(hourlyUsage);
		BigDecimal yesterdayGasUsage = sumGasUsage(yesterdayHourlyUsage);
		BigDecimal yesterdayWaterUsage = sumWaterUsage(yesterdayHourlyUsage);
		BigDecimal gasTotal = monthlyUsage == null ? BigDecimal.ZERO : zeroIfNull(monthlyUsage.getGasUsageM3());
		BigDecimal waterTotal = monthlyUsage == null ? BigDecimal.ZERO : zeroIfNull(monthlyUsage.getWaterUsageTon());

		UtilityUsageMetricResponse metrics = new UtilityUsageMetricResponse(
			gasUsage,
			gasTotal,
			changeRate(gasUsage, yesterdayGasUsage),
			waterUsage,
			waterTotal,
			changeRate(waterUsage, yesterdayWaterUsage)
		);

		return new UtilityUsageDashboardResponse(
			plantId,
			resolvedDate,
			metrics,
			hourlyUsage,
			energyMapper.findUtilityMeterStatuses(plantId, from, to),
			energyMapper.findUtilityDailyUsagePatterns(plantId, patternFrom, to)
		);
	}

	@Transactional(readOnly = true)
	public List<EnergyFacilityLineUsageResponse> getFacilityLineUsages(
		Long plantId,
		FacilityType facilityType,
		LocalDate targetDate
	) {
		if (plantId == null || facilityType == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate requestedDate = targetDate == null ? LocalDate.now() : targetDate;
		LocalDate usageDate = energyMapper.findFacilityLineSummaryDate(plantId, facilityType, requestedDate)
			.or(() -> energyMapper.findLatestFacilityLineSummaryDate(plantId, facilityType))
			.orElse(requestedDate);

		return energyMapper.findFacilityLineUsages(
			plantId,
			facilityType,
			usageDate,
			usageDate.minusDays(1),
			usageDate.withDayOfMonth(1),
			usageDate.plusMonths(1).withDayOfMonth(1),
			usageDate.atStartOfDay(),
			usageDate.plusDays(1).atStartOfDay()
		);
	}

	@Transactional(readOnly = true)
	public EnergyFacilityDetailResponse getFacilityDetail(
		Long plantId,
		Long facilityId,
		EnergyType energyType,
		LocalDate from,
		LocalDate to
	) {
		EnergyType resolvedEnergyType = energyType == null ? EnergyType.ELECTRICITY : energyType;
		LocalDate resolvedTo = to == null ? LocalDate.now() : to;
		LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(6) : from;
		if (resolvedFrom.isAfter(resolvedTo)) {
			LocalDate previousFrom = resolvedFrom;
			resolvedFrom = resolvedTo;
			resolvedTo = previousFrom;
		}

		Facility facility = facilityMapper.findById(facilityId)
			.filter(foundFacility -> foundFacility.getPlantId().equals(plantId))
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));

		List<EnergySummary> summaries = energyMapper.findSummaries(
			plantId,
			facilityId,
			SummaryType.DAILY,
			resolvedFrom.atStartOfDay(),
			resolvedTo.atTime(LocalTime.MAX)
		);
		List<EnergySummary> measurementDailySums = energyMapper.findMeasurementDailySums(
			plantId,
			facilityId,
			resolvedFrom.atStartOfDay(),
			resolvedTo.atTime(LocalTime.MAX)
		);
		Map<LocalDate, EnergySummary> summariesByDate = summaries.stream()
			.collect(Collectors.toMap(
				summary -> summary.getSummaryAt().toLocalDate(),
				summary -> summary,
				(left, right) -> right
			));
		Map<LocalDate, EnergySummary> measurementSumsByDate = measurementDailySums.stream()
			.collect(Collectors.toMap(
				summary -> summary.getSummaryAt().toLocalDate(),
				summary -> summary,
				(left, right) -> right
			));
		LocalDate currentDate = resolvedTo;

		List<EnergyUsagePointResponse> chart = resolvedFrom.datesUntil(resolvedTo.plusDays(1))
			.map(date -> {
				EnergySummary summary = summaryForDate(date, currentDate, summariesByDate, measurementSumsByDate);
				return new EnergyUsagePointResponse(
					date,
					summary == null ? date.atStartOfDay() : summary.getSummaryAt(),
					usageOf(resolvedEnergyType, summary)
				);
			})
			.toList();

		List<EnergyUsageLogResponse> logs = chart.stream()
			.sorted(Comparator.comparing(EnergyUsagePointResponse::date).reversed())
			.map(point -> {
				BigDecimal previousUsage = usageByDate(chart, point.date().minusDays(1));
				return new EnergyUsageLogResponse(
					point.summaryAt(),
					point.usage(),
					point.usage().subtract(previousUsage),
					changeRate(point.usage(), previousUsage)
				);
			})
			.toList();

		BigDecimal todayUsage = usageByDate(chart, resolvedTo);
		BigDecimal yesterdayUsage = usageByDate(chart, resolvedTo.minusDays(1));
		LocalDateTime latestMeasuredAt = energyMapper.findLatestMeasurement(plantId, facilityId)
			.map(EnergyMeasurement::getMeasuredAt)
			.orElseGet(() -> chart.stream()
				.filter(point -> point.usage().compareTo(BigDecimal.ZERO) > 0)
				.reduce((previous, current) -> current)
				.map(EnergyUsagePointResponse::summaryAt)
				.orElse(null));

		return new EnergyFacilityDetailResponse(
			plantId,
			facilityId,
			facility.getName(),
			facility.getFacilityType(),
			facility.getStatus(),
			resolvedEnergyType,
			resolvedEnergyType.unit(),
			todayUsage,
			yesterdayUsage,
			todayUsage.subtract(yesterdayUsage),
			changeRate(todayUsage, yesterdayUsage),
			latestMeasuredAt,
			chart,
			logs
		);
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private BigDecimal toBigDecimal(Double value) {
		return value == null ? null : BigDecimal.valueOf(value);
	}

	private BigDecimal usageOf(EnergyType energyType, EnergySummary summary) {
		if (summary == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal usage = energyType.usage(summary);
		return usage == null ? BigDecimal.ZERO : usage;
	}

	private EnergySummary summaryForDate(
		LocalDate date,
		LocalDate currentDate,
		Map<LocalDate, EnergySummary> summariesByDate,
		Map<LocalDate, EnergySummary> measurementSumsByDate
	) {
		EnergySummary measurementSum = measurementSumsByDate.get(date);
		if (date.equals(currentDate) && measurementSum != null) {
			return measurementSum;
		}
		return summariesByDate.getOrDefault(date, measurementSum);
	}

	private BigDecimal usageByDate(List<EnergyUsagePointResponse> chart, LocalDate date) {
		return chart.stream()
			.filter(point -> point.date().equals(date))
			.findFirst()
			.map(EnergyUsagePointResponse::usage)
			.orElse(BigDecimal.ZERO);
	}

	private BigDecimal electricityUsageForDate(Long plantId, Long facilityId, LocalDate date) {
		List<EnergySummary> measurementSums = energyMapper.findMeasurementDailySums(
			plantId,
			facilityId,
			date.atStartOfDay(),
			date.atTime(LocalTime.MAX)
		);
		if (!measurementSums.isEmpty()) {
			return zeroIfNull(measurementSums.get(0).getElectricityKwh());
		}

		return energyMapper.findSummaries(
				plantId,
				facilityId,
				SummaryType.DAILY,
				date.atStartOfDay(),
				date.atTime(LocalTime.MAX)
			)
			.stream()
			.findFirst()
			.map(EnergySummary::getElectricityKwh)
			.map(this::zeroIfNull)
			.orElse(BigDecimal.ZERO);
	}

	private PeakPowerPeriodRange resolvePeakPowerPeriodRange(LocalDate targetDate, PeakPowerPeriod period) {
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

	private PeakPowerPeriodRange previousPeakPowerPeriodRange(PeakPowerPeriodRange periodRange) {
		return switch (periodRange.period()) {
			case DAY -> new PeakPowerPeriodRange(PeakPowerPeriod.DAY, periodRange.from().minusDays(1), periodRange.from());
			case WEEK -> new PeakPowerPeriodRange(PeakPowerPeriod.WEEK, periodRange.from().minusWeeks(1), periodRange.from());
			case MONTH -> new PeakPowerPeriodRange(PeakPowerPeriod.MONTH, periodRange.from().minusMonths(1), periodRange.from());
		};
	}

	private List<PeakPowerTrendPoint> findPeakTrend(
		Long plantId,
		LocalDateTime from,
		LocalDateTime to,
		PeakPowerPeriod period
	) {
		if (period == PeakPowerPeriod.DAY) {
			return energyMapper.findPeakPowerTrend(plantId, from, to);
		}
		return energyMapper.findPeakPowerDailyTrend(plantId, from, to);
	}

	private BigDecimal changeRate(BigDecimal currentUsage, BigDecimal previousUsage) {
		if (previousUsage == null || previousUsage.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return currentUsage.subtract(previousUsage)
			.multiply(BigDecimal.valueOf(100))
			.divide(previousUsage, 1, RoundingMode.HALF_UP);
	}

	private BigDecimal rateOf(BigDecimal value, BigDecimal baseline) {
		if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return zeroIfNull(value)
			.multiply(BigDecimal.valueOf(100))
			.divide(baseline, 1, RoundingMode.HALF_UP);
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private BigDecimal sumGasUsage(List<UtilityHourlyUsage> hourlyUsage) {
		return hourlyUsage.stream()
			.map(UtilityHourlyUsage::getGasUsageM3)
			.map(this::zeroIfNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal sumWaterUsage(List<UtilityHourlyUsage> hourlyUsage) {
		return hourlyUsage.stream()
			.map(UtilityHourlyUsage::getWaterUsageTon)
			.map(this::zeroIfNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal averagePeak(List<PeakPowerTrendPoint> trend) {
		if (trend.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return trend.stream()
			.map(PeakPowerTrendPoint::getAverageKw)
			.map(this::zeroIfNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.divide(BigDecimal.valueOf(trend.size()), 2, RoundingMode.HALF_UP);
	}

	private BigDecimal peakThresholdForPlant(Long plantId) {
		long facilityCount = facilityMapper.findByPlantId(plantId).stream()
			.filter(facility -> isLineFacility(facility.getId(), plantId))
			.count();
		if (facilityCount <= 0) {
			return FACILITY_PEAK_THRESHOLD_KW;
		}
		return FACILITY_PEAK_THRESHOLD_KW.multiply(BigDecimal.valueOf(facilityCount));
	}

	private boolean isLineFacility(Long facilityId, Long plantId) {
		if (facilityId == null || plantId == null || facilityId < 10000) {
			return false;
		}
		long sequence = facilityId % 10000;
		return sequence >= 1 && sequence <= 24 && facilityId / 10000 == plantId;
	}

	private record PeakPowerPeriodRange(PeakPowerPeriod period, LocalDate from, LocalDate to) {
	}

	private void createRealtimeAlarms(EnergyMeasurement current, EnergyMeasurement previous) {
		createPeakAlarm(current);

		if (previous == null) {
			return;
		}

		BigDecimal electricityDelta = delta(current.getElectricityKwh(), previous.getElectricityKwh());
		createDeltaAlarm(
			current,
			AlarmType.ELECTRICITY,
			electricityDelta,
			ELECTRICITY_WARNING_DELTA,
			ELECTRICITY_CRITICAL_DELTA,
			"kWh",
			"전기 사용량이 급증했습니다."
		);

		BigDecimal gasDelta = delta(current.getGasM3(), previous.getGasM3());
		createDeltaAlarm(
			current,
			AlarmType.GAS,
			gasDelta,
			GAS_WARNING_DELTA,
			GAS_CRITICAL_DELTA,
			"m3",
			"가스 사용량이 급증했습니다."
		);

		BigDecimal waterDelta = delta(current.getWaterTon(), previous.getWaterTon());
		createDeltaAlarm(
			current,
			AlarmType.WATER,
			waterDelta,
			WATER_WARNING_DELTA,
			WATER_CRITICAL_DELTA,
			"ton",
			"용수 사용량이 급증했습니다."
		);

		createMeterReverseAlarm(current, previous);
	}

	private void createPeakAlarm(EnergyMeasurement current) {
		BigDecimal peakKw = zeroIfNull(current.getPeakKw());
		if (peakKw.compareTo(FACILITY_PEAK_THRESHOLD_KW) < 0) {
			return;
		}

		BigDecimal criticalThreshold = FACILITY_PEAK_THRESHOLD_KW.multiply(BigDecimal.valueOf(1.1));
		AlarmLevel level = peakKw.compareTo(criticalThreshold) >= 0 ? AlarmLevel.CRITICAL : AlarmLevel.WARNING;
		createAlarmIfNotRecent(
			current,
			AlarmType.PEAK,
			level,
			peakKw,
			FACILITY_PEAK_THRESHOLD_KW,
			String.format("%s 피크 전력이 기준값을 초과했습니다.", facilityLabel(current.getFacilityId()))
		);
	}

	private void createDeltaAlarm(
		EnergyMeasurement current,
		AlarmType alarmType,
		BigDecimal delta,
		BigDecimal warningThreshold,
		BigDecimal criticalThreshold,
		String unit,
		String message
	) {
		if (delta.compareTo(warningThreshold) < 0) {
			return;
		}

		AlarmLevel level = delta.compareTo(criticalThreshold) >= 0 ? AlarmLevel.CRITICAL : AlarmLevel.WARNING;
		BigDecimal threshold = level == AlarmLevel.CRITICAL ? criticalThreshold : warningThreshold;
		createAlarmIfNotRecent(
			current,
			alarmType,
			level,
			delta,
			threshold,
			String.format("%s %s 증가량: %s %s", facilityLabel(current.getFacilityId()), message, delta, unit)
		);
	}

	private void createMeterReverseAlarm(EnergyMeasurement current, EnergyMeasurement previous) {
		if (!isReversed(current.getElectricityKwh(), previous.getElectricityKwh())
			&& !isReversed(current.getGasM3(), previous.getGasM3())
			&& !isReversed(current.getWaterTon(), previous.getWaterTon())) {
			return;
		}

		createAlarmIfNotRecent(
			current,
			AlarmType.FACILITY,
			AlarmLevel.WARNING,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			String.format("%s 누적 계측값이 직전 수집값보다 감소했습니다. 계측기 리셋 또는 이상 여부를 확인하세요.", facilityLabel(current.getFacilityId()))
		);
	}

	private void createAlarmIfNotRecent(
		EnergyMeasurement measurement,
		AlarmType alarmType,
		AlarmLevel alarmLevel,
		BigDecimal value,
		BigDecimal thresholdValue,
		String message
	) {
		LocalDateTime since = measurement.getMeasuredAt().minusMinutes(15);
		long recentCount = alarmMapper.countRecentOccurred(
			measurement.getPlantId(),
			measurement.getFacilityId(),
			alarmType,
			since
		);
		if (recentCount > 0) {
			return;
		}

		Alarm alarm = new Alarm();
		alarm.setPlantId(measurement.getPlantId());
		alarm.setFacilityId(measurement.getFacilityId());
		alarm.setAlarmType(alarmType);
		alarm.setAlarmLevel(alarmLevel);
		alarm.setMessage(message);
		alarm.setValue(value);
		alarm.setThresholdValue(thresholdValue);
		alarm.setOccurredAt(measurement.getMeasuredAt());
		alarm.setStatus(AlarmStatus.OCCURRED);
		alarmMapper.insert(alarm);
	}

	private String facilityLabel(Long facilityId) {
		return facilityMapper.findById(facilityId)
			.map(Facility::getName)
			.orElse("설비 " + facilityId);
	}

	private BigDecimal delta(BigDecimal current, BigDecimal previous) {
		BigDecimal currentValue = zeroIfNull(current);
		BigDecimal previousValue = zeroIfNull(previous);
		if (currentValue.compareTo(previousValue) < 0) {
			return BigDecimal.ZERO;
		}
		return currentValue.subtract(previousValue);
	}

	private boolean isReversed(BigDecimal current, BigDecimal previous) {
		return current != null && previous != null && current.compareTo(previous) < 0;
	}

	private LocalDateTime toLocalDateTime(EnergyMeasurementMessage message) {
		if (message.getMeasuredAt() == null) {
			return LocalDateTime.now();
		}
		return LocalDateTime.ofInstant(message.getMeasuredAt(), ZoneId.systemDefault());
	}
}
