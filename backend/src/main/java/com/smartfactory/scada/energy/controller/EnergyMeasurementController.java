package com.smartfactory.scada.energy.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;
import com.smartfactory.scada.energy.service.EnergyMeasurementRedisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/energy/latest")
public class EnergyMeasurementController {

	private final EnergyMeasurementRedisService energyMeasurementRedisService;

	@GetMapping("/plants/{plantId}/facilities/{facilityId}")
	public ResponseEntity<EnergyMeasurementMessage> findLatest(
		@PathVariable Long plantId,
		@PathVariable Long facilityId
	) {
		// Return the cached latest value when present, otherwise respond with 404.
		Optional<EnergyMeasurementMessage> latestMeasurement =
			energyMeasurementRedisService.findLatest(plantId, facilityId);

		return latestMeasurement
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
