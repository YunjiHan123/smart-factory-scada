package com.smartfactory.scada.smwp.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.energy.domain.EnergyType;
import com.smartfactory.scada.smwp.dto.SmwpDailyEnergyResponse;
import com.smartfactory.scada.smwp.dto.SmwpEnergyComparisonResponse;
import com.smartfactory.scada.smwp.dto.SmwpHourlyEnergyResponse;
import com.smartfactory.scada.smwp.service.SmwpEnergyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/smwp/energy")
public class SmwpEnergyController {

	private final SmwpEnergyService smwpEnergyService;

	@GetMapping("/daily")
	public SmwpDailyEnergyResponse getDailyEnergy(
		@RequestParam String plantName,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return smwpEnergyService.getDailyEnergy(plantName, date);
	}

	@GetMapping("/hourly")
	public SmwpHourlyEnergyResponse getHourlyEnergy(
		@RequestParam String plantName,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return smwpEnergyService.getHourlyEnergy(plantName, date);
	}

	@GetMapping("/comparison")
	public SmwpEnergyComparisonResponse getEnergyComparison(
		@RequestParam String plantName,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam(defaultValue = "ELECTRICITY") EnergyType energyType
	) {
		return smwpEnergyService.getEnergyComparison(plantName, date, energyType);
	}
}
