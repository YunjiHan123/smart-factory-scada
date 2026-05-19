package com.smartfactory.scada.energy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfactory.scada.energy.domain.ElectricityBillDataStatus;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonRowResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;
import com.smartfactory.scada.energy.mapper.EnergyMapper;

@ExtendWith(MockitoExtension.class)
class ElectricityBillComparisonServiceTest {

	@Mock
	private EnergyMapper energyMapper;

	private ElectricityBillComparisonService electricityBillComparisonService;

	@BeforeEach
	void setUp() {
		electricityBillComparisonService = new ElectricityBillComparisonService(
			energyMapper,
			new ElectricityBillCalculator()
		);
	}

	@Test
	void compareRanksAllTariffsAndCalculatesSavingsFromDefaultBaseTariff() {
		Long plantId = 1L;
		LocalDate date = LocalDate.of(2026, 6, 1);
		LocalDateTime from = date.withDayOfMonth(1).atStartOfDay();
		LocalDateTime to = date.plusMonths(1).withDayOfMonth(1).atStartOfDay();
		given(energyMapper.findElectricityBillingDemandPeak(plantId, from, to))
			.willReturn(Optional.of(demandPeak(LocalDateTime.of(2026, 6, 1, 14, 15), "100")));
		given(energyMapper.findElectricityBillingUsageIntervals(plantId, from, to))
			.willReturn(List.of(
				usageInterval(LocalDateTime.of(2026, 6, 1, 23, 0), "10"),
				usageInterval(LocalDateTime.of(2026, 6, 1, 10, 0), "5"),
				usageInterval(LocalDateTime.of(2026, 6, 1, 14, 0), "2")
			));

		ElectricityBillComparisonResponse response = electricityBillComparisonService.compare(
			plantId,
			date,
			null,
			null
		);

		assertThat(response.period()).isEqualTo(PeakPowerPeriod.MONTH);
		assertThat(response.dataStatus()).isEqualTo(ElectricityBillDataStatus.OK);
		assertThat(response.baseTariffCode()).isEqualTo(ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II);
		assertThat(response.baseEstimatedTotalKrw()).isEqualByComparingTo("834437");
		assertThat(response.bestTariffCode()).isEqualTo(ElectricityTariffCode.HIGH_VOLTAGE_C_OPTION_I);
		assertThat(response.bestEstimatedTotalKrw()).isEqualByComparingTo("661688");
		assertThat(response.estimatedSavingKrw()).isEqualByComparingTo("172749");
		assertThat(response.comparisons()).hasSize(9);
		assertThat(response.comparisons().get(0).recommended()).isTrue();
		assertThat(response.comparisons().get(0).rank()).isEqualTo(1);
		assertThat(response.comparisons())
			.filteredOn(row -> row.tariffCode() == ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II)
			.singleElement()
			.satisfies(row -> {
				ElectricityBillComparisonRowResponse baseRow = (ElectricityBillComparisonRowResponse) row;
				assertThat(baseRow.savingKrw()).isEqualByComparingTo(BigDecimal.ZERO);
				assertThat(baseRow.savingRate()).isEqualByComparingTo(BigDecimal.ZERO);
			});
		verify(energyMapper, times(1)).findElectricityBillingDemandPeak(plantId, from, to);
		verify(energyMapper, times(1)).findElectricityBillingUsageIntervals(plantId, from, to);
	}

	@Test
	void compareReturnsEmptyStatusWhenThereIsNoBillingData() {
		Long plantId = 1L;
		LocalDate date = LocalDate.of(2026, 5, 19);
		LocalDateTime from = date.withDayOfMonth(1).atStartOfDay();
		LocalDateTime to = date.plusMonths(1).withDayOfMonth(1).atStartOfDay();
		given(energyMapper.findElectricityBillingDemandPeak(plantId, from, to)).willReturn(Optional.empty());
		given(energyMapper.findElectricityBillingUsageIntervals(plantId, from, to)).willReturn(List.of());

		ElectricityBillComparisonResponse response = electricityBillComparisonService.compare(
			plantId,
			date,
			PeakPowerPeriod.MONTH,
			ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II
		);

		assertThat(response.dataStatus()).isEqualTo(ElectricityBillDataStatus.EMPTY);
		assertThat(response.baseEstimatedTotalKrw()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.bestTariffCode()).isNull();
		assertThat(response.comparisons()).isEmpty();
		assertThat(response.assumptions()).isNotEmpty();
	}

	private ElectricityBillingDemandPeak demandPeak(LocalDateTime measuredAt, String peakKw) {
		ElectricityBillingDemandPeak demandPeak = new ElectricityBillingDemandPeak();
		demandPeak.setMeasuredAt(measuredAt);
		demandPeak.setPeakKw(new BigDecimal(peakKw));
		return demandPeak;
	}

	private ElectricityBillingUsageInterval usageInterval(LocalDateTime measuredAt, String usageKwh) {
		ElectricityBillingUsageInterval interval = new ElectricityBillingUsageInterval();
		interval.setMeasuredAt(measuredAt);
		interval.setUsageKwh(new BigDecimal(usageKwh));
		return interval;
	}
}
