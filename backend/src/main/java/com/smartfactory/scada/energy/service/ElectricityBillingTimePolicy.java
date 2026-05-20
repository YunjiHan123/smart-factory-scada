package com.smartfactory.scada.energy.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;

public final class ElectricityBillingTimePolicy {

	private static final LocalTime OFF_PEAK_START = LocalTime.of(22, 0);
	private static final LocalTime OFF_PEAK_END = LocalTime.of(8, 0);

	private ElectricityBillingTimePolicy() {
	}

	public static ElectricitySeason seasonOf(LocalDateTime measuredAt) {
		return ElectricitySeason.from(measuredAt.toLocalDate());
	}

	public static ElectricityLoadPeriod loadPeriodOf(LocalDateTime measuredAt) {
		LocalTime time = measuredAt.toLocalTime();
		if (!time.isBefore(OFF_PEAK_START) || time.isBefore(OFF_PEAK_END)) {
			return ElectricityLoadPeriod.OFF_PEAK;
		}

		ElectricitySeason season = seasonOf(measuredAt);
		if (season == ElectricitySeason.WINTER && (isWithin(time, 9, 12) || isWithin(time, 16, 19))) {
			return ElectricityLoadPeriod.ON_PEAK;
		}
		if (season != ElectricitySeason.WINTER && (isWithin(time, 11, 12) || isWithin(time, 13, 18))) {
			return ElectricityLoadPeriod.ON_PEAK;
		}
		return ElectricityLoadPeriod.MID_PEAK;
	}

	private static boolean isWithin(LocalTime time, int startHour, int endHour) {
		return !time.isBefore(LocalTime.of(startHour, 0)) && time.isBefore(LocalTime.of(endHour, 0));
	}
}
