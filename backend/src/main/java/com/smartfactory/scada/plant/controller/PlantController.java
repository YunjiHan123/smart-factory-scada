package com.smartfactory.scada.plant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.plant.dto.PlantResponse;
import com.smartfactory.scada.plant.service.PlantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

	private final PlantService plantService;

	@GetMapping
	public List<PlantResponse> getPlants() {
		return plantService.getPlants();
	}

	@GetMapping("/{plantId}")
	public PlantResponse getPlant(@PathVariable Long plantId) {
		return plantService.getPlant(plantId);
	}
}
