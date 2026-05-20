package com.smartfactory.scada.smwp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.plant.domain.Plant;
import com.smartfactory.scada.plant.mapper.PlantMapper;
import com.smartfactory.scada.smwp.dto.SmwpDailyEnergyResponse;
import com.smartfactory.scada.smwp.dto.SmwpDailyEnergyUsage;
import com.smartfactory.scada.smwp.dto.SmwpEnergyComparisonResponse;
import com.smartfactory.scada.smwp.dto.SmwpHourlyEnergyPoint;
import com.smartfactory.scada.smwp.dto.SmwpHourlyEnergyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmwpEnergyService {

	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

	private final PlantMapper plantMapper;
	private final EnergyMapper energyMapper;
	private final Map<String, Plant> plantCache = new ConcurrentHashMap<>();

	public SmwpDailyEnergyResponse getDailyEnergy(String plantName, LocalDate date) {
		if (plantName == null || plantName.isBlank() || date == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		Plant plant = findPlantByName(plantName);

		SmwpDailyEnergyUsage usage = energyMapper.findSmwpDailyEnergy(
			plant.getId(),
			date.atStartOfDay(),
			date.plusDays(1).atStartOfDay()
		).orElseGet(() -> emptyUsage(plant.getId()));
		usage = fillDailyFromSummaryIfEmpty(plant.getId(), date, usage);

		return new SmwpDailyEnergyResponse(
			plant.getId(),
			plant.getName(),
			date,
			zeroIfNull(usage.getElectricityKwh()),
			zeroIfNull(usage.getGasM3()),
			zeroIfNull(usage.getWaterTon()),
			zeroIfNull(usage.getSolarKwh()),
			usage.getLatestMeasuredAt()
		);
	}

	public SmwpHourlyEnergyResponse getHourlyEnergy(String plantName, LocalDate date) {
		if (plantName == null || plantName.isBlank() || date == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		Plant plant = findPlantByName(plantName);
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();
		LocalDate today = LocalDate.now(SERVICE_ZONE);
		boolean isToday = date.equals(today);
		LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
		int currentHour = now.getHour();

		Map<Integer, SmwpHourlyEnergyPoint> pointsByHour = new HashMap<>();
		List<SmwpHourlyEnergyPoint> hourlyPoints = energyMapper.findSmwpHourlyEnergy(plant.getId(), from, to);
		if (hourlyPoints.isEmpty()) {
			hourlyPoints = energyMapper.findSmwpHourlyEnergyFromMeasurements(plant.getId(), from, to);
		}
		for (SmwpHourlyEnergyPoint point : hourlyPoints) {
			if (point.getHour() != null) {
				pointsByHour.put(point.getHour(), point);
			}
		}

		SmwpDailyEnergyUsage currentHourUsage = null;
		if (isToday) {
			LocalDateTime hourStart = now.withMinute(0).withSecond(0).withNano(0);
			currentHourUsage = energyMapper.findSmwpDailyEnergy(
				plant.getId(),
				hourStart,
				now
			).orElseGet(() -> emptyUsage(plant.getId()));
		}

		List<String> labels = new ArrayList<>();
		List<BigDecimal> electricity = new ArrayList<>();
		List<BigDecimal> gas = new ArrayList<>();
		List<BigDecimal> water = new ArrayList<>();
		List<BigDecimal> solar = new ArrayList<>();
		LocalDateTime latestMeasuredAt = currentHourUsage == null ? null : currentHourUsage.getLatestMeasuredAt();

		for (int hour = 0; hour <= 24; hour++) {
			labels.add(String.format("%02d:00", hour));
			if (hour == 24 || (isToday && hour > currentHour)) {
				addZeros(electricity, gas, water, solar);
				continue;
			}

			if (isToday && hour == currentHour) {
				electricity.add(zeroIfNull(currentHourUsage.getElectricityKwh()));
				gas.add(zeroIfNull(currentHourUsage.getGasM3()));
				water.add(zeroIfNull(currentHourUsage.getWaterTon()));
				solar.add(zeroIfNull(currentHourUsage.getSolarKwh()));
				continue;
			}

			SmwpHourlyEnergyPoint point = pointsByHour.get(hour);
			if (point == null) {
				addZeros(electricity, gas, water, solar);
			} else {
				electricity.add(zeroIfNull(point.getElectricityKwh()));
				gas.add(zeroIfNull(point.getGasM3()));
				water.add(zeroIfNull(point.getWaterTon()));
				solar.add(zeroIfNull(point.getSolarKwh()));
			}
		}

		return new SmwpHourlyEnergyResponse(
			plant.getId(),
			plant.getName(),
			date,
			labels,
			electricity,
			gas,
			water,
			solar,
			latestMeasuredAt
		);
	}

	public SmwpEnergyComparisonResponse getEnergyComparison(String plantName, LocalDate date, EnergyType energyType) {
		if (plantName == null || plantName.isBlank() || date == null || energyType == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		Plant plant = findPlantByName(plantName);
		SmwpDailyEnergyResponse dailyEnergy = getDailyEnergy(plantName, date);
		BigDecimal currentUsage = usageOf(dailyEnergy, energyType);
		BigDecimal previousMonthAverage = previousMonthDailyAverage(plant.getId(), date, energyType);
		BigDecimal changeRate = changeRate(currentUsage, previousMonthAverage);

		return new SmwpEnergyComparisonResponse(
			plant.getId(),
			plant.getName(),
			date,
			energyType,
			energyType.unit(),
			previousMonthAverage,
			currentUsage,
			changeRate,
			changeRate.signum() >= 0 ? "UP" : "DOWN"
		);
	}

	private SmwpDailyEnergyUsage emptyUsage(Long plantId) {
		SmwpDailyEnergyUsage usage = new SmwpDailyEnergyUsage();
		usage.setPlantId(plantId);
		usage.setElectricityKwh(BigDecimal.ZERO);
		usage.setGasM3(BigDecimal.ZERO);
		usage.setWaterTon(BigDecimal.ZERO);
		usage.setSolarKwh(BigDecimal.ZERO);
		return usage;
	}

	private SmwpDailyEnergyUsage fillDailyFromSummaryIfEmpty(
		Long plantId,
		LocalDate date,
		SmwpDailyEnergyUsage usage
	) {
		if (usage.getLatestMeasuredAt() != null || date.equals(LocalDate.now(SERVICE_ZONE))) {
			return usage;
		}

		List<EnergySummary> summaries = energyMapper.findSummaries(
			plantId,
			null,
			SummaryType.DAILY,
			date.atStartOfDay(),
			date.plusDays(1).atStartOfDay()
		);
		if (summaries.isEmpty()) {
			return usage;
		}

		EnergySummary summary = summaries.get(0);
		usage.setElectricityKwh(zeroIfNull(summary.getElectricityKwh()));
		usage.setGasM3(zeroIfNull(summary.getGasM3()));
		usage.setWaterTon(zeroIfNull(summary.getWaterTon()));
		usage.setSolarKwh(zeroIfNull(summary.getSolarKwh()));
		usage.setLatestMeasuredAt(summary.getSummaryAt());
		return usage;
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private BigDecimal previousMonthDailyAverage(Long plantId, LocalDate date, EnergyType energyType) {
		LocalDate currentMonthStart = date.withDayOfMonth(1);
		LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
		LocalDateTime from = previousMonthStart.atStartOfDay();
		LocalDateTime to = currentMonthStart.atStartOfDay().minusSeconds(1);

		List<EnergySummary> summaries = energyMapper.findSummaries(
			plantId,
			null,
			SummaryType.DAILY,
			from,
			to
		);
		if (summaries.isEmpty()) {
			return BigDecimal.ZERO;
		}

		BigDecimal total = summaries.stream()
			.map(summary -> zeroIfNull(energyType.usage(summary)))
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return total.divide(BigDecimal.valueOf(summaries.size()), 2, RoundingMode.HALF_UP);
	}

	private BigDecimal changeRate(BigDecimal currentUsage, BigDecimal baseUsage) {
		if (baseUsage == null || baseUsage.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}

		return zeroIfNull(currentUsage)
			.subtract(baseUsage)
			.multiply(BigDecimal.valueOf(100))
			.divide(baseUsage, 1, RoundingMode.HALF_UP);
	}

	private BigDecimal usageOf(SmwpDailyEnergyResponse dailyEnergy, EnergyType energyType) {
		return switch (energyType) {
			case ELECTRICITY -> zeroIfNull(dailyEnergy.electricityKwh());
			case GAS -> zeroIfNull(dailyEnergy.gasM3());
			case WATER -> zeroIfNull(dailyEnergy.waterTon());
			case SOLAR -> zeroIfNull(dailyEnergy.solarKwh());
		};
	}

	private void addZeros(
		List<BigDecimal> electricity,
		List<BigDecimal> gas,
		List<BigDecimal> water,
		List<BigDecimal> solar
	) {
		electricity.add(BigDecimal.ZERO);
		gas.add(BigDecimal.ZERO);
		water.add(BigDecimal.ZERO);
		solar.add(BigDecimal.ZERO);
	}

	private Plant findPlantByName(String plantName) {
		String key = normalize(plantName);
		Plant cachedPlant = plantCache.get(key);
		if (cachedPlant != null) {
			return cachedPlant;
		}

		Plant plant = plantMapper.findAll().stream()
			.filter(candidate -> normalize(candidate.getName()).equals(key))
			.findFirst()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
		plantCache.put(key, plant);
		return plant;
	}

	private String normalize(String value) {
		return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
			.replaceAll("\\s+", "")
			.toLowerCase(Locale.ROOT);
	}
}
