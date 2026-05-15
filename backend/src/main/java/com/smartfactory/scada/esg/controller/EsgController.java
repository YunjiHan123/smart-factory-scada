package com.smartfactory.scada.esg.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.esg.dto.EsgScoreResponse;
import com.smartfactory.scada.esg.service.EsgService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/esg")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class EsgController {

	private final EsgService esgService;

	@GetMapping("/scores")
	public List<EsgScoreResponse> getScores(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetMonth
	) {
		return esgService.getScores(targetMonth);
	}

	@GetMapping("/scores/plants/{plantId}/latest")
	public EsgScoreResponse getLatestScore(@PathVariable Long plantId) {
		return esgService.getLatestScore(plantId);
	}
}
