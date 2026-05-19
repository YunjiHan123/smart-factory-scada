package com.smartfactory.scada.energy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfactory.scada.energy.domain.ElectricityLoadPeriod;
import com.smartfactory.scada.energy.domain.ElectricitySeason;
import com.smartfactory.scada.energy.domain.ElectricityTariffCode;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.dto.ElectricityBillEstimateResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillUsageBreakdownResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillingDemandPeak;
import com.smartfactory.scada.energy.dto.ElectricityBillingUsageInterval;
import com.smartfactory.scada.energy.mapper.EnergyMapper;

@ExtendWith(MockitoExtension.class)
class ElectricityBillServiceTest {

	@Mock
	private EnergyMapper energyMapper;

	@InjectMocks
	private ElectricityBillService electricityBillService;

	@Test
	void estimateUsesDefaultHighVoltageAOptionTwoTariff() {
		Long plantId = 1L;
		LocalDate date = LocalDate.of(2026, 6, 1);
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();
		given(energyMapper.findElectricityBillingDemandPeak(plantId, from, to))
			.willReturn(Optional.of(demandPeak(LocalDateTime.of(2026, 6, 1, 14, 15), "100")));
		given(energyMapper.findElectricityBillingUsageIntervals(plantId, from, to))
			.willReturn(List.of(
				usageInterval(LocalDateTime.of(2026, 6, 1, 23, 0), "10"),
				usageInterval(LocalDateTime.of(2026, 6, 1, 10, 0), "5"),
				usageInterval(LocalDateTime.of(2026, 6, 1, 14, 0), "2"),
				usageInterval(LocalDateTime.of(2026, 6, 1, 15, 0), "-9")
			));

		ElectricityBillEstimateResponse response = electricityBillService.estimate(
			plantId,
			date,
			PeakPowerPeriod.DAY,
			null
		);

		assertThat(response.tariffCode()).isEqualTo(ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II);
		assertThat(response.billingDemandKw()).isEqualByComparingTo("100.00");
		assertThat(response.basicRateKrwPerKw()).isEqualByComparingTo("8320");
		assertThat(response.basicChargeKrw()).isEqualByComparingTo("832000");
		assertThat(response.energyChargeKrw()).isEqualByComparingTo("2437");
		assertThat(response.estimatedTotalKrw()).isEqualByComparingTo("834437");
		assertThat(response.demandUnit()).isEqualTo("kW");
		assertThat(response.usageUnit()).isEqualTo("kWh");
		assertThat(response.currencyUnit()).isEqualTo("KRW");
		assertThat(response.assumptions()).isNotEmpty();
		assertBreakdown(response, ElectricitySeason.SUMMER, ElectricityLoadPeriod.OFF_PEAK, "10.00", "116.0", "1160");
		assertBreakdown(response, ElectricitySeason.SUMMER, ElectricityLoadPeriod.MID_PEAK, "5.00", "163.8", "819");
		assertBreakdown(response, ElectricitySeason.SUMMER, ElectricityLoadPeriod.ON_PEAK, "2.00", "229.0", "458");
	}

	@Test
	void estimateReturnsZeroAmountsWhenThereIsNoBillingData() {
		Long plantId = 1L;
		LocalDate date = LocalDate.of(2026, 1, 15);
		LocalDateTime from = LocalDate.of(2026, 1, 1).atStartOfDay();
		LocalDateTime to = LocalDate.of(2026, 2, 1).atStartOfDay();
		given(energyMapper.findElectricityBillingDemandPeak(plantId, from, to)).willReturn(Optional.empty());
		given(energyMapper.findElectricityBillingUsageIntervals(plantId, from, to)).willReturn(List.of());

		ElectricityBillEstimateResponse response = electricityBillService.estimate(
			plantId,
			date,
			PeakPowerPeriod.MONTH,
			ElectricityTariffCode.HIGH_VOLTAGE_A_OPTION_II
		);

		assertThat(response.periodFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(response.periodTo()).isEqualTo(LocalDate.of(2026, 1, 31));
		assertThat(response.billingDemandKw()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.basicChargeKrw()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.energyChargeKrw()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.estimatedTotalKrw()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.usageBreakdown()).isEmpty();
	}

	private void assertBreakdown(
		ElectricityBillEstimateResponse response,
		ElectricitySeason season,
		ElectricityLoadPeriod loadPeriod,
		String usageKwh,
		String rateKrwPerKwh,
		String chargeKrw
	) {
		assertThat(response.usageBreakdown())
			.filteredOn(item -> item.season() == season && item.loadPeriod() == loadPeriod)
			.singleElement()
			.satisfies(item -> {
				ElectricityBillUsageBreakdownResponse breakdown = (ElectricityBillUsageBreakdownResponse) item;
				assertThat(breakdown.usageKwh()).isEqualByComparingTo(usageKwh);
				assertThat(breakdown.rateKrwPerKwh()).isEqualByComparingTo(rateKrwPerKwh);
				assertThat(breakdown.chargeKrw()).isEqualByComparingTo(chargeKrw);
			});
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
