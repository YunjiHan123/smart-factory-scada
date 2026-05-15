package com.smartfactory.scada.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse.EnergyUsageLogResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse.EnergyUsagePointResponse;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.dto.EnergyMeasurementResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.energy.dto.PeakPowerDashboardResponse;
import com.smartfactory.scada.energy.dto.PeakPowerFacilityRanking;
import com.smartfactory.scada.energy.dto.PeakPowerHistory;
import com.smartfactory.scada.energy.dto.PeakPowerMetricResponse;
import com.smartfactory.scada.energy.dto.PeakPowerTrendPoint;
import com.smartfactory.scada.energy.dto.UtilityHourlyUsage;
import com.smartfactory.scada.energy.dto.UtilityUsageDashboardResponse;
import com.smartfactory.scada.energy.dto.UtilityUsageMetricResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.facility.domain.Facility;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnergyService {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 500;
	private static final BigDecimal FACILITY_PEAK_THRESHOLD_KW = BigDecimal.valueOf(1400);

	private final EnergyMapper energyMapper;
	private final FacilityMapper facilityMapper;

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

		energyMapper.insertMeasurement(measurement);
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
	public PeakPowerDashboardResponse getPeakDashboard(Long plantId, LocalDate targetDate) {
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		LocalDate resolvedDate = targetDate == null ? LocalDate.now() : targetDate;
		LocalDateTime from = resolvedDate.atStartOfDay();
		LocalDateTime to = resolvedDate.plusDays(1).atStartOfDay();
		BigDecimal thresholdKw = peakThresholdForPlant(plantId);

		List<PeakPowerTrendPoint> trend = energyMapper.findPeakPowerTrend(plantId, from, to);
		List<PeakPowerFacilityRanking> ranking = energyMapper.findPeakPowerFacilityRanking(plantId, from, to, 5);
		List<PeakPowerHistory> history = energyMapper.findPeakPowerHistory(
			plantId,
			from,
			to,
			thresholdKw,
			10
		);

		EnergyMeasurement latestMeasurement = energyMapper.findLatestPlantMeasurement(plantId, from, to).orElse(null);
		PeakPowerTrendPoint latestInterval = trend.isEmpty() ? null : trend.get(trend.size() - 1);
		BigDecimal currentKw = latestMeasurement == null ? BigDecimal.ZERO : zeroIfNull(latestMeasurement.getPeakKw());
		BigDecimal intervalAverageKw = latestInterval == null ? BigDecimal.ZERO : zeroIfNull(latestInterval.getAverageKw());
		BigDecimal intervalMaxKw = latestInterval == null ? BigDecimal.ZERO : zeroIfNull(latestInterval.getMaxKw());

		PeakPowerMetricResponse metrics = new PeakPowerMetricResponse(
			currentKw,
			rateOf(currentKw, thresholdKw),
			intervalAverageKw,
			intervalMaxKw,
			thresholdKw,
			latestMeasurement == null ? null : latestMeasurement.getMeasuredAt(),
			latestInterval == null ? null : latestInterval.getMeasuredAt()
		);

		return new PeakPowerDashboardResponse(
			plantId,
			resolvedDate,
			metrics,
			trend,
			ranking,
			history
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

		List<UtilityHourlyUsage> hourlyUsage = energyMapper.findUtilityHourlyUsage(plantId, from, to);
		List<UtilityHourlyUsage> yesterdayHourlyUsage = energyMapper.findUtilityHourlyUsage(plantId, yesterdayFrom, from);
		EnergyMeasurement latestUtilityMeasurement = energyMapper.findLatestPlantUtilityMeasurement(plantId, from, to)
			.orElse(null);

		BigDecimal gasUsage = sumGasUsage(hourlyUsage);
		BigDecimal waterUsage = sumWaterUsage(hourlyUsage);
		BigDecimal yesterdayGasUsage = sumGasUsage(yesterdayHourlyUsage);
		BigDecimal yesterdayWaterUsage = sumWaterUsage(yesterdayHourlyUsage);
		BigDecimal gasTotal = latestUtilityMeasurement == null ? BigDecimal.ZERO : zeroIfNull(latestUtilityMeasurement.getGasM3());
		BigDecimal waterTotal = latestUtilityMeasurement == null ? BigDecimal.ZERO : zeroIfNull(latestUtilityMeasurement.getWaterTon());

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
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
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

		List<EnergyUsagePointResponse> chart = resolvedFrom.datesUntil(resolvedTo.plusDays(1))
			.map(date -> {
				EnergySummary summary = summaryForDate(date, resolvedTo, summariesByDate, measurementSumsByDate);
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

	private BigDecimal peakThresholdForPlant(Long plantId) {
		int facilityCount = facilityMapper.findByPlantId(plantId).size();
		if (facilityCount <= 0) {
			return FACILITY_PEAK_THRESHOLD_KW;
		}
		return FACILITY_PEAK_THRESHOLD_KW.multiply(BigDecimal.valueOf(facilityCount));
	}

	private LocalDateTime toLocalDateTime(EnergyMeasurementMessage message) {
		if (message.getMeasuredAt() == null) {
			return LocalDateTime.now();
		}
		return LocalDateTime.ofInstant(message.getMeasuredAt(), ZoneId.systemDefault());
	}
}
