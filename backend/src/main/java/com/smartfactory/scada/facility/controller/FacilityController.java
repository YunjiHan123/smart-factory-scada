package com.smartfactory.scada.facility.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.facility.dto.FacilityResponse;
import com.smartfactory.scada.facility.service.FacilityService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class FacilityController {

	private final FacilityService facilityService;

	@GetMapping("/plants/{plantId}/facilities")
	public List<FacilityResponse> getFacilities(@PathVariable Long plantId) {
		return facilityService.getFacilities(plantId);
	}

	@GetMapping("/facilities/{facilityId}")
	public FacilityResponse getFacility(@PathVariable Long facilityId) {
		return facilityService.getFacility(facilityId);
	}
}
