package com.smartfactory.scada.plant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.plant.dto.PlantResponse;
import com.smartfactory.scada.plant.mapper.PlantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlantService {

	private final PlantMapper plantMapper;

	@Transactional(readOnly = true)
	public List<PlantResponse> getPlants() {
		return plantMapper.findAll()
			.stream()
			.map(PlantResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public PlantResponse getPlant(Long plantId) {
		return plantMapper.findById(plantId)
			.map(PlantResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
	}
}
