package com.smartfactory.scada.facility.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.facility.dto.FacilityResponse;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityService {

	private final FacilityMapper facilityMapper;

	@Transactional(readOnly = true)
	public List<FacilityResponse> getFacilities(Long plantId) {
		return facilityMapper.findByPlantId(plantId)
			.stream()
			.map(FacilityResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public FacilityResponse getFacility(Long facilityId) {
		return facilityMapper.findById(facilityId)
			.map(FacilityResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
	}
}
