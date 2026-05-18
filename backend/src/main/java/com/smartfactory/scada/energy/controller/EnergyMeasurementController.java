package com.smartfactory.scada.energy.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.dto.EnergyFacilityDetailResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityLineUsageResponse;
import com.smartfactory.scada.energy.dto.EnergyMeasurementResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.energy.dto.PeakPowerDashboardResponse;
import com.smartfactory.scada.energy.dto.UtilityUsageDashboardResponse;
import com.smartfactory.scada.energy.service.EnergyService;
import com.smartfactory.scada.facility.domain.FacilityType;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/energy")
@SecurityRequirement(name = "bearerAuth")
public class EnergyMeasurementController {

	private final EnergyService energyService;

	@GetMapping("/measurements")
	public List<EnergyMeasurementResponse> getMeasurements(
		@RequestParam(required = false) Long plantId,
		@RequestParam(required = false) Long facilityId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
		@RequestParam(required = false) Integer limit
	) {
		return energyService.getMeasurements(plantId, facilityId, from, to, limit);
	}

	@GetMapping("/summaries")
	public List<EnergySummaryResponse> getSummaries(
		@RequestParam(required = false) Long plantId,
		@RequestParam(required = false) Long facilityId,
		@RequestParam(required = false) SummaryType summaryType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		return energyService.getSummaries(plantId, facilityId, summaryType, from, to);
	}

	@GetMapping("/facility-detail")
	public EnergyFacilityDetailResponse getFacilityDetail(
		@RequestParam Long plantId,
		@RequestParam Long facilityId,
		@RequestParam(required = false) EnergyType energyType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return energyService.getFacilityDetail(plantId, facilityId, energyType, from, to);
	}

	@GetMapping("/facility-line")
	public List<EnergyFacilityLineUsageResponse> getFacilityLineUsages(
		@RequestParam Long plantId,
		@RequestParam FacilityType facilityType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return energyService.getFacilityLineUsages(plantId, facilityType, date);
	}

	@GetMapping("/peak-dashboard")
	public PeakPowerDashboardResponse getPeakDashboard(
		@RequestParam Long plantId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return energyService.getPeakDashboard(plantId, date);
	}

	@GetMapping("/utility-dashboard")
	public UtilityUsageDashboardResponse getUtilityDashboard(
		@RequestParam Long plantId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return energyService.getUtilityDashboard(plantId, date);
	}

	@GetMapping("/latest/plants/{plantId}/facilities/{facilityId}")
	public ResponseEntity<EnergyMeasurementResponse> findLatest(
		@PathVariable Long plantId,
		@PathVariable Long facilityId
	) {
		return energyService.getLatestMeasurement(plantId, facilityId)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
