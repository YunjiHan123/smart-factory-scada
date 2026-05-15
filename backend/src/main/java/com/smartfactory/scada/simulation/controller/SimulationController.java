package com.smartfactory.scada.simulation.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.simulation.dto.SimulationRequest;
import com.smartfactory.scada.simulation.dto.SimulationResponse;
import com.smartfactory.scada.simulation.service.SimulationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/simulations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SimulationController {

	private final SimulationService simulationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SimulationResponse simulate(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody SimulationRequest request
	) {
		return simulationService.simulate(authenticatedUser, request);
	}

	@GetMapping
	public List<SimulationResponse> getRecent(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(required = false) Long plantId,
		@RequestParam(required = false) Integer limit
	) {
		return simulationService.getRecent(authenticatedUser, plantId, limit);
	}
}
