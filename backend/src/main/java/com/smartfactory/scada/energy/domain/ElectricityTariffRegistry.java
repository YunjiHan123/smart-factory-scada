package com.smartfactory.scada.energy.domain;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ElectricityTariffRegistry {

	public static final ElectricityTariffCode DEFAULT_TARIFF_CODE = ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II;
	public static final String SOURCE = "사용자 첨부 한전 산업용전력(을) 요금표";

	private static final Map<ElectricityTariffCode, ElectricityTariffPlan> PLANS = plans();

	private ElectricityTariffRegistry() {
	}

	public static ElectricityTariffPlan defaultPlan() {
		return PLANS.get(DEFAULT_TARIFF_CODE);
	}

	public static Optional<ElectricityTariffPlan> find(ElectricityTariffCode code) {
		if (code == null) {
			return Optional.of(defaultPlan());
		}
		return Optional.ofNullable(PLANS.get(code));
	}

	private static Map<ElectricityTariffCode, ElectricityTariffPlan> plans() {
		EnumMap<ElectricityTariffCode, ElectricityTariffPlan> plans = new EnumMap<>(ElectricityTariffCode.class);
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_I, "7220",
			rates("121.5", "169.3", "234.5", "121.5", "138.9", "156.4", "128.5", "169.5", "210.1"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II, "8320",
			rates("116.0", "163.8", "229.0", "116.0", "133.4", "150.9", "123.0", "164.0", "204.6"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_III, "9810",
			rates("113.1", "163.2", "216.6", "113.1", "132.1", "142.6", "120.1", "163.4", "193.4"));

		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_B_OPTION_I, "6630",
			rates("131.4", "178.6", "242.9", "131.4", "148.6", "165.7", "138.4", "178.6", "217.9"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_B_OPTION_II, "7380",
			rates("127.6", "174.8", "239.1", "127.6", "144.8", "161.9", "134.6", "174.8", "214.1"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_B_OPTION_III, "8190",
			rates("125.9", "173.1", "237.5", "125.9", "143.2", "160.3", "133.0", "173.1", "212.4"));

		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_C_OPTION_I, "6590",
			rates("130.9", "178.7", "242.7", "130.9", "148.7", "165.9", "137.8", "178.3", "218.0"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_C_OPTION_II, "7520",
			rates("126.2", "174.0", "238.0", "126.2", "144.0", "161.2", "133.1", "173.6", "213.3"));
		add(plans, ElectricityTariffCode.HIGH_VOLTAGE_C_OPTION_III, "8090",
			rates("125.1", "172.9", "236.9", "125.1", "142.9", "160.1", "132.0", "172.5", "212.2"));
		return Map.copyOf(plans);
	}

	private static void add(
		Map<ElectricityTariffCode, ElectricityTariffPlan> plans,
		ElectricityTariffCode code,
		String basicRateKrwPerKw,
		Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> rates
	) {
		plans.put(code, new ElectricityTariffPlan(
			code,
			code.displayName(),
			decimal(basicRateKrwPerKw),
			rates
		));
	}

	private static Map<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> rates(
		String summerOffPeak,
		String summerMidPeak,
		String summerOnPeak,
		String springAutumnOffPeak,
		String springAutumnMidPeak,
		String springAutumnOnPeak,
		String winterOffPeak,
		String winterMidPeak,
		String winterOnPeak
	) {
		EnumMap<ElectricitySeason, Map<ElectricityLoadPeriod, BigDecimal>> rates = new EnumMap<>(ElectricitySeason.class);
		rates.put(ElectricitySeason.SUMMER, loadRates(summerOffPeak, summerMidPeak, summerOnPeak));
		rates.put(ElectricitySeason.SPRING_AUTUMN, loadRates(springAutumnOffPeak, springAutumnMidPeak, springAutumnOnPeak));
		rates.put(ElectricitySeason.WINTER, loadRates(winterOffPeak, winterMidPeak, winterOnPeak));
		return rates;
	}

	private static Map<ElectricityLoadPeriod, BigDecimal> loadRates(
		String offPeak,
		String midPeak,
		String onPeak
	) {
		EnumMap<ElectricityLoadPeriod, BigDecimal> rates = new EnumMap<>(ElectricityLoadPeriod.class);
		rates.put(ElectricityLoadPeriod.OFF_PEAK, decimal(offPeak));
		rates.put(ElectricityLoadPeriod.MID_PEAK, decimal(midPeak));
		rates.put(ElectricityLoadPeriod.ON_PEAK, decimal(onPeak));
		return rates;
	}

	private static BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
