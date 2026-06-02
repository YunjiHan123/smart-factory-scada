package com.smartfactory.scada.energy.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;

class ElectricityBillingTimePolicyTest {

	@Test
	void seasonOfClassifiesMonthsByKepcoIndustrialTariffSeason() {
		assertThat(ElectricityBillingTimePolicy.seasonOf(LocalDateTime.of(2026, 7, 1, 0, 0)))
			.isEqualTo(ElectricitySeason.SUMMER);
		assertThat(ElectricityBillingTimePolicy.seasonOf(LocalDateTime.of(2026, 4, 1, 0, 0)))
			.isEqualTo(ElectricitySeason.SPRING_AUTUMN);
		assertThat(ElectricityBillingTimePolicy.seasonOf(LocalDateTime.of(2026, 10, 1, 0, 0)))
			.isEqualTo(ElectricitySeason.SPRING_AUTUMN);
		assertThat(ElectricityBillingTimePolicy.seasonOf(LocalDateTime.of(2026, 12, 1, 0, 0)))
			.isEqualTo(ElectricitySeason.WINTER);
		assertThat(ElectricityBillingTimePolicy.seasonOf(LocalDateTime.of(2026, 2, 1, 0, 0)))
			.isEqualTo(ElectricitySeason.WINTER);
	}

	@Test
	void loadPeriodOfClassifiesOffPeakBoundaries() {
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 1, 21, 59)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 1, 22, 0)))
			.isEqualTo(ElectricityLoadPeriod.OFF_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 2, 7, 59)))
			.isEqualTo(ElectricityLoadPeriod.OFF_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 2, 8, 0)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
	}

	@Test
	void loadPeriodOfClassifiesSummerAndSpringAutumnOnPeakWindows() {
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 1, 11, 0)))
			.isEqualTo(ElectricityLoadPeriod.ON_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 6, 1, 12, 0)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 9, 1, 13, 0)))
			.isEqualTo(ElectricityLoadPeriod.ON_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 9, 1, 18, 0)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
	}

	@Test
	void loadPeriodOfClassifiesWinterOnPeakWindows() {
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 1, 1, 9, 0)))
			.isEqualTo(ElectricityLoadPeriod.ON_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 1, 1, 12, 0)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 1, 1, 16, 0)))
			.isEqualTo(ElectricityLoadPeriod.ON_PEAK);
		assertThat(ElectricityBillingTimePolicy.loadPeriodOf(LocalDateTime.of(2026, 1, 1, 19, 0)))
			.isEqualTo(ElectricityLoadPeriod.MID_PEAK);
	}
}
