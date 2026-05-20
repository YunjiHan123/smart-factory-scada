package com.smartfactory.scada.energy.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.smartfactory.scada.energy.domain.PeakPowerPeriod;

final class ElectricityBillingPeriodResolver {

	private ElectricityBillingPeriodResolver() {
	}

	static PeriodRange resolve(LocalDate targetDate, PeakPowerPeriod period, PeakPowerPeriod defaultPeriod) {
		PeakPowerPeriod resolvedPeriod = period == null ? defaultPeriod : period;
		return switch (resolvedPeriod) {
			case DAY -> new PeriodRange(resolvedPeriod, targetDate, targetDate.plusDays(1));
			case WEEK -> {
				LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				yield new PeriodRange(resolvedPeriod, weekStart, weekStart.plusWeeks(1));
			}
			case MONTH -> {
				LocalDate monthStart = targetDate.withDayOfMonth(1);
				yield new PeriodRange(resolvedPeriod, monthStart, monthStart.plusMonths(1));
			}
		};
	}

	record PeriodRange(PeakPowerPeriod period, LocalDate fromInclusive, LocalDate toExclusive) {

		LocalDate displayTo() {
			return toExclusive.minusDays(1);
		}
	}
}
