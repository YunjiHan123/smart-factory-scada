package com.smartfactory.scada.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.dashboard.dto.DashboardOverviewResponse;
import com.smartfactory.scada.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/overview")
	public DashboardOverviewResponse getOverview(@RequestParam(required = false) Long plantId) {
		return dashboardService.getOverview(plantId);
	}
}
