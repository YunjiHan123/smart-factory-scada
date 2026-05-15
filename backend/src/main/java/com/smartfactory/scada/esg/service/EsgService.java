package com.smartfactory.scada.esg.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.esg.domain.EsgEnergyAggregate;
import com.smartfactory.scada.esg.dto.EsgScoreResponse;
import com.smartfactory.scada.esg.dto.EsgEnvironmentDashboardResponse;
import com.smartfactory.scada.esg.dto.EsgEnvironmentDashboardResponse.EsgEnvironmentAlertResponse;
import com.smartfactory.scada.esg.dto.EsgEnvironmentDashboardResponse.EsgEnvironmentLogicResponse;
import com.smartfactory.scada.esg.dto.EsgEnvironmentDashboardResponse.EsgEnvironmentMetricResponse;
import com.smartfactory.scada.esg.dto.EsgEnvironmentDashboardResponse.EsgEnvironmentPlantResponse;
import com.smartfactory.scada.esg.mapper.EsgMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EsgService {

	private static final BigDecimal ELECTRICITY_CARBON_FACTOR = BigDecimal.valueOf(0.47);
	private static final BigDecimal GAS_CARBON_FACTOR = BigDecimal.valueOf(2.176);
	private static final BigDecimal FACILITY_PEAK_THRESHOLD_KW = BigDecimal.valueOf(1400);

	private final EsgMapper esgMapper;

	@Transactional(readOnly = true)
	public List<EsgScoreResponse> getScores(LocalDate targetMonth) {
		return esgMapper.findScores(targetMonth)
			.stream()
			.map(EsgScoreResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public EsgScoreResponse getLatestScore(Long plantId) {
		return esgMapper.findLatestByPlantId(plantId)
			.map(EsgScoreResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
	}

	@Transactional(readOnly = true)
	public EsgEnvironmentDashboardResponse getEnvironmentDashboard(Long plantId, LocalDate from, LocalDate to) {
		LocalDate resolvedTo = to == null ? LocalDate.now() : to;
		LocalDate resolvedFrom = from == null ? resolvedTo : from;
		if (resolvedFrom.isAfter(resolvedTo)) {
			LocalDate previousFrom = resolvedFrom;
			resolvedFrom = resolvedTo;
			resolvedTo = previousFrom;
		}

		LocalDateTime fromDateTime = resolvedFrom.atStartOfDay();
		LocalDateTime toDateTime = resolvedTo.plusDays(1).atStartOfDay();
		List<EsgEnergyAggregate> current = environmentAggregates(fromDateTime, toDateTime);
		List<EsgEnergyAggregate> previous = environmentAggregates(
			resolvedFrom.minusMonths(1).atStartOfDay(),
			resolvedTo.minusMonths(1).plusDays(1).atStartOfDay()
		);
		Map<Long, EsgEnergyAggregate> previousByPlantId = previous.stream()
			.collect(Collectors.toMap(EsgEnergyAggregate::getPlantId, Function.identity(), (left, right) -> right));

		List<EsgEnvironmentPlantResponse> rankedPlants = rankPlants(current, previousByPlantId);
		EsgEnvironmentPlantResponse selected = selectedPlant(plantId, rankedPlants);
		List<EsgEnvironmentMetricResponse> metrics = metricsFor(selected);

		return new EsgEnvironmentDashboardResponse(
			resolvedFrom,
			resolvedTo,
			selected == null ? plantId : selected.plantId(),
			rankedPlants,
			selected,
			metrics,
			alertsFor(rankedPlants),
			new EsgEnvironmentLogicResponse(
				"항목별 사용량은 사업장 간 분포와 내부 기준값을 함께 보정해 0~10점으로 정규화합니다.",
				"탄소 30%, 용수 20%, 태양광 20%, 피크전력 20%, 전력/가스 효율 10% 가중 평균입니다.",
				"9.0 이상 AAA, 8.0 AA, 7.0 A, 6.0 BBB, 5.0 BB, 4.0 B, 그 외 CCC입니다."
			)
		);
	}

	private List<EsgEnergyAggregate> environmentAggregates(LocalDateTime from, LocalDateTime to) {
		return esgMapper.findEnvironmentAggregatesFromSummaries(null, from, to);
	}

	private List<EsgEnvironmentPlantResponse> rankPlants(
		List<EsgEnergyAggregate> current,
		Map<Long, EsgEnergyAggregate> previousByPlantId
	) {
		List<ScoredAggregate> scored = current.stream()
			.map(aggregate -> scoreAggregate(aggregate, previousByPlantId.get(aggregate.getPlantId()), current))
			.sorted(Comparator.comparing(ScoredAggregate::totalScore).reversed()
				.thenComparing(scoredAggregate -> scoredAggregate.aggregate().getPlantName()))
			.toList();

		return IntStream.range(0, scored.size())
			.mapToObj(index -> scored.get(index).toResponse(index + 1))
			.toList();
	}

	private ScoredAggregate scoreAggregate(
		EsgEnergyAggregate aggregate,
		EsgEnergyAggregate previous,
		List<EsgEnergyAggregate> population
	) {
		BigDecimal carbonEmission = carbonEmission(aggregate);
		BigDecimal previousCarbonEmission = previous == null ? BigDecimal.ZERO : carbonEmission(previous);
		BigDecimal peakThreshold = peakThreshold(aggregate);
		BigDecimal peakReductionRate = peakThreshold.compareTo(BigDecimal.ZERO) == 0
			? BigDecimal.ZERO
			: peakThreshold.subtract(zeroIfNull(aggregate.getIntervalMaxPeakKw()))
				.multiply(BigDecimal.valueOf(100))
				.divide(peakThreshold, 1, RoundingMode.HALF_UP);

		BigDecimal carbonScore = lowerIsBetter(carbonEmission, population, this::carbonEmission);
		BigDecimal waterScore = lowerIsBetter(zeroIfNull(aggregate.getWaterTon()), population, item -> zeroIfNull(item.getWaterTon()));
		BigDecimal solarScore = higherIsBetter(zeroIfNull(aggregate.getSolarKwh()), population, item -> zeroIfNull(item.getSolarKwh()));
		BigDecimal peakScore = peakScore(zeroIfNull(aggregate.getIntervalMaxPeakKw()), peakThreshold);
		BigDecimal electricityScore = lowerIsBetter(
			zeroIfNull(aggregate.getElectricityKwh()),
			population,
			item -> zeroIfNull(item.getElectricityKwh())
		);
		BigDecimal gasScore = lowerIsBetter(zeroIfNull(aggregate.getGasM3()), population, item -> zeroIfNull(item.getGasM3()));
		BigDecimal totalScore = carbonScore.multiply(BigDecimal.valueOf(0.30))
			.add(waterScore.multiply(BigDecimal.valueOf(0.20)))
			.add(solarScore.multiply(BigDecimal.valueOf(0.20)))
			.add(peakScore.multiply(BigDecimal.valueOf(0.20)))
			.add(electricityScore.add(gasScore).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(0.10)));

		return new ScoredAggregate(
			aggregate,
			round1(totalScore),
			grade(totalScore),
			round1(changeRate(carbonEmission, previousCarbonEmission)),
			round2(carbonEmission),
			round1(changeRate(zeroIfNull(aggregate.getWaterTon()), previous == null ? BigDecimal.ZERO : zeroIfNull(previous.getWaterTon()))),
			round1(changeRate(zeroIfNull(aggregate.getSolarKwh()), previous == null ? BigDecimal.ZERO : zeroIfNull(previous.getSolarKwh()))),
			round1(peakReductionRate),
			round1(carbonScore),
			round1(waterScore),
			round1(solarScore),
			round1(peakScore),
			round1(electricityScore),
			round1(gasScore)
		);
	}

	private EsgEnvironmentPlantResponse selectedPlant(Long plantId, List<EsgEnvironmentPlantResponse> plants) {
		if (plants.isEmpty()) {
			return null;
		}
		if (plantId == null) {
			return plants.get(0);
		}
		return plants.stream()
			.filter(plant -> plant.plantId().equals(plantId))
			.findFirst()
			.orElse(plants.get(0));
	}

	private List<EsgEnvironmentMetricResponse> metricsFor(EsgEnvironmentPlantResponse plant) {
		if (plant == null) {
			return List.of();
		}
		return List.of(
			new EsgEnvironmentMetricResponse("carbon", "탄소배출", plant.carbonScore(), "tCO2e", plant.carbonEmission(), plant.changeRate(), "전력/가스 사용량"),
			new EsgEnvironmentMetricResponse("water", "용수사용", plant.waterScore(), "ton", plant.waterUsage(), plant.waterChangeRate(), "용수 계측값"),
			new EsgEnvironmentMetricResponse("solar", "태양광발전", plant.solarScore(), "kWh", plant.solarGeneration(), plant.solarChangeRate(), "태양광 계측값"),
			new EsgEnvironmentMetricResponse("peak", "피크전력", plant.peakScore(), "kW", plant.intervalMaxPeakKw(), plant.peakReductionRate(), "15분 최대전력"),
			new EsgEnvironmentMetricResponse("electricity", "전력사용", plant.electricityScore(), "kWh", plant.electricityKwh(), BigDecimal.ZERO, "전력 계측값"),
			new EsgEnvironmentMetricResponse("gas", "가스사용", plant.gasScore(), "m3", plant.gasM3(), BigDecimal.ZERO, "가스 계측값")
		);
	}

	private List<EsgEnvironmentAlertResponse> alertsFor(List<EsgEnvironmentPlantResponse> plants) {
		return plants.stream()
			.flatMap(plant -> {
				List<EsgEnvironmentAlertResponse> alerts = new java.util.ArrayList<>();
				if (plant.measurementCount() == null || plant.measurementCount() == 0) {
					alerts.add(new EsgEnvironmentAlertResponse("danger", "데이터 누락", "조회 기간 내 에너지 계측 데이터가 없습니다.", plant.plantId(), plant.plantName()));
				}
				if (plant.peakReductionRate().compareTo(BigDecimal.ZERO) < 0) {
					alerts.add(new EsgEnvironmentAlertResponse("danger", "피크전력 초과", "15분 최대전력이 내부 기준을 초과했습니다.", plant.plantId(), plant.plantName()));
				}
				if (plant.changeRate().compareTo(BigDecimal.valueOf(20)) > 0) {
					alerts.add(new EsgEnvironmentAlertResponse("warn", "에너지 사용 급증", "전월 대비 탄소배출 추정치가 20% 이상 증가했습니다.", plant.plantId(), plant.plantName()));
				}
				return alerts.stream();
			})
			.limit(8)
			.toList();
	}

	private BigDecimal carbonEmission(EsgEnergyAggregate aggregate) {
		return zeroIfNull(aggregate.getElectricityKwh()).multiply(ELECTRICITY_CARBON_FACTOR)
			.add(zeroIfNull(aggregate.getGasM3()).multiply(GAS_CARBON_FACTOR))
			.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
	}

	private BigDecimal peakThreshold(EsgEnergyAggregate aggregate) {
		int facilityCount = aggregate.getFacilityCount() == null || aggregate.getFacilityCount() <= 0
			? 1
			: aggregate.getFacilityCount();
		return FACILITY_PEAK_THRESHOLD_KW.multiply(BigDecimal.valueOf(facilityCount));
	}

	private BigDecimal lowerIsBetter(
		BigDecimal value,
		List<EsgEnergyAggregate> population,
		Function<EsgEnergyAggregate, BigDecimal> extractor
	) {
		MinMax minMax = minMax(population, extractor);
		if (minMax.max().compareTo(minMax.min()) == 0) {
			return value.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(6.5) : BigDecimal.valueOf(8);
		}
		BigDecimal ratio = value.subtract(minMax.min())
			.divide(minMax.max().subtract(minMax.min()), 4, RoundingMode.HALF_UP);
		return clampScore(BigDecimal.TEN.subtract(ratio.multiply(BigDecimal.valueOf(5))));
	}

	private BigDecimal higherIsBetter(
		BigDecimal value,
		List<EsgEnergyAggregate> population,
		Function<EsgEnergyAggregate, BigDecimal> extractor
	) {
		MinMax minMax = minMax(population, extractor);
		if (minMax.max().compareTo(minMax.min()) == 0) {
			return value.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(5) : BigDecimal.valueOf(8);
		}
		BigDecimal ratio = value.subtract(minMax.min())
			.divide(minMax.max().subtract(minMax.min()), 4, RoundingMode.HALF_UP);
		return clampScore(BigDecimal.valueOf(5).add(ratio.multiply(BigDecimal.valueOf(5))));
	}

	private BigDecimal peakScore(BigDecimal peakKw, BigDecimal thresholdKw) {
		if (thresholdKw.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.valueOf(7);
		}
		BigDecimal usageRate = peakKw.multiply(BigDecimal.valueOf(100))
			.divide(thresholdKw, 2, RoundingMode.HALF_UP);
		if (usageRate.compareTo(BigDecimal.valueOf(70)) <= 0) {
			return BigDecimal.TEN;
		}
		if (usageRate.compareTo(BigDecimal.valueOf(100)) <= 0) {
			return BigDecimal.TEN.subtract(usageRate.subtract(BigDecimal.valueOf(70))
				.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(3)));
		}
		return clampScore(BigDecimal.valueOf(7).subtract(usageRate.subtract(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(2))));
	}

	private MinMax minMax(List<EsgEnergyAggregate> population, Function<EsgEnergyAggregate, BigDecimal> extractor) {
		List<BigDecimal> values = population.stream()
			.map(extractor)
			.map(this::zeroIfNull)
			.toList();
		return new MinMax(
			values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
			values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
		);
	}

	private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
		if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return current.subtract(previous).multiply(BigDecimal.valueOf(100))
			.divide(previous, 2, RoundingMode.HALF_UP);
	}

	private String grade(BigDecimal score) {
		if (score.compareTo(BigDecimal.valueOf(9)) >= 0) {
			return "AAA";
		}
		if (score.compareTo(BigDecimal.valueOf(8)) >= 0) {
			return "AA";
		}
		if (score.compareTo(BigDecimal.valueOf(7)) >= 0) {
			return "A";
		}
		if (score.compareTo(BigDecimal.valueOf(6)) >= 0) {
			return "BBB";
		}
		if (score.compareTo(BigDecimal.valueOf(5)) >= 0) {
			return "BB";
		}
		if (score.compareTo(BigDecimal.valueOf(4)) >= 0) {
			return "B";
		}
		return "CCC";
	}

	private BigDecimal clampScore(BigDecimal value) {
		return value.max(BigDecimal.ZERO).min(BigDecimal.TEN);
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private BigDecimal round1(BigDecimal value) {
		return zeroIfNull(value).setScale(1, RoundingMode.HALF_UP);
	}

	private BigDecimal round2(BigDecimal value) {
		return zeroIfNull(value).setScale(2, RoundingMode.HALF_UP);
	}

	private record MinMax(BigDecimal min, BigDecimal max) {
	}

	private record ScoredAggregate(
		EsgEnergyAggregate aggregate,
		BigDecimal totalScore,
		String grade,
		BigDecimal changeRate,
		BigDecimal carbonEmission,
		BigDecimal waterChangeRate,
		BigDecimal solarChangeRate,
		BigDecimal peakReductionRate,
		BigDecimal carbonScore,
		BigDecimal waterScore,
		BigDecimal solarScore,
		BigDecimal peakScore,
		BigDecimal electricityScore,
		BigDecimal gasScore
	) {
		private EsgEnvironmentPlantResponse toResponse(int rank) {
			return new EsgEnvironmentPlantResponse(
				rank,
				aggregate.getPlantId(),
				aggregate.getPlantName(),
				aggregate.getLatitude(),
				aggregate.getLongitude(),
				totalScore,
				grade,
				changeRate,
				waterChangeRate,
				solarChangeRate,
				carbonEmission,
				zero(aggregate.getWaterTon()),
				zero(aggregate.getSolarKwh()),
				zero(aggregate.getCurrentPeakKw()),
				zero(aggregate.getIntervalMaxPeakKw()),
				peakReductionRate,
				zero(aggregate.getElectricityKwh()),
				zero(aggregate.getGasM3()),
				waterScore,
				solarScore,
				peakScore,
				carbonScore,
				electricityScore,
				gasScore,
				aggregate.getFacilityCount(),
				aggregate.getMeasurementCount(),
				aggregate.getLatestMeasuredAt()
			);
		}

		private static BigDecimal zero(BigDecimal value) {
			return value == null ? BigDecimal.ZERO : value;
		}
	}
}
