package com.smartfactory.scada.energy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.energy.dto.EnergyFacilityLineUsageResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.facility.domain.FacilityType;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

	@Mock
	private EnergyMapper energyMapper;

	@Mock
	private FacilityMapper facilityMapper;

	@InjectMocks
	private EnergyService energyService;

	@Test
	void getFacilityLineUsagesUsesRequestedDateWhenDateIsProvided() {
		LocalDate targetDate = LocalDate.of(2026, 5, 13);
		List<EnergyFacilityLineUsageResponse> expectedResponses = List.of(new EnergyFacilityLineUsageResponse());
		givenFindFacilityLineUsages(1L, FacilityType.PRESS, targetDate, expectedResponses);

		List<EnergyFacilityLineUsageResponse> responses =
			energyService.getFacilityLineUsages(1L, FacilityType.PRESS, EnergyType.ELECTRICITY, targetDate);

		assertThat(responses).isSameAs(expectedResponses);
		then(energyMapper).should(never()).findLatestFacilityLineSummaryDate(1L, FacilityType.PRESS);
	}

	@Test
	void getFacilityLineUsagesFallsBackToLatestSummaryDateWhenDateIsMissing() {
		LocalDate latestSummaryDate = LocalDate.of(2026, 5, 13);
		List<EnergyFacilityLineUsageResponse> expectedResponses = List.of(new EnergyFacilityLineUsageResponse());
		given(energyMapper.findLatestFacilityLineSummaryDate(1L, FacilityType.BODY))
			.willReturn(Optional.of(latestSummaryDate));
		givenFindFacilityLineUsages(1L, FacilityType.BODY, latestSummaryDate, expectedResponses);

		List<EnergyFacilityLineUsageResponse> responses =
			energyService.getFacilityLineUsages(1L, FacilityType.BODY, EnergyType.ELECTRICITY, null);

		assertThat(responses).isSameAs(expectedResponses);
		then(energyMapper).should().findLatestFacilityLineSummaryDate(1L, FacilityType.BODY);
	}

	@Test
	void getFacilityLineUsagesFallsBackToLatestMeasurementDateWhenSummaryDateIsMissing() {
		LocalDate latestMeasurementDate = LocalDate.of(2026, 5, 18);
		List<EnergyFacilityLineUsageResponse> expectedResponses = List.of(new EnergyFacilityLineUsageResponse());
		given(energyMapper.findLatestFacilityLineSummaryDate(1L, FacilityType.PAINT))
			.willReturn(Optional.empty());
		given(energyMapper.findLatestFacilityLineMeasurementDate(1L, FacilityType.PAINT))
			.willReturn(Optional.of(latestMeasurementDate));
		givenFindFacilityLineUsages(1L, FacilityType.PAINT, latestMeasurementDate, expectedResponses);

		List<EnergyFacilityLineUsageResponse> responses =
			energyService.getFacilityLineUsages(1L, FacilityType.PAINT, EnergyType.ELECTRICITY, null);

		assertThat(responses).isSameAs(expectedResponses);
	}

	@Test
	void getFacilityLineUsagesThrowsValidationErrorWhenRequiredArgumentsAreMissing() {
		assertThatThrownBy(() -> energyService.getFacilityLineUsages(
			null,
			FacilityType.PRESS,
			EnergyType.ELECTRICITY,
			LocalDate.now()
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR)
			);
		assertThatThrownBy(() -> energyService.getFacilityLineUsages(
			1L,
			null,
			EnergyType.ELECTRICITY,
			LocalDate.now()
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR)
			);

		then(energyMapper).shouldHaveNoInteractions();
	}

	private void givenFindFacilityLineUsages(
		Long plantId,
		FacilityType facilityType,
		LocalDate usageDate,
		List<EnergyFacilityLineUsageResponse> responses
	) {
		given(energyMapper.findFacilityLineUsages(
			eq(plantId),
			eq(facilityType),
			eq(EnergyType.ELECTRICITY.name()),
			eq(usageDate),
			eq(usageDate.minusDays(1)),
			eq(usageDate.withDayOfMonth(1)),
			eq(usageDate.plusMonths(1).withDayOfMonth(1)),
			eq(usageDate.atStartOfDay()),
			eq(usageDate.plusDays(1).atStartOfDay())
		)).willReturn(responses);
	}
}
