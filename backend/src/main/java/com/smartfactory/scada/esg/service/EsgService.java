package com.smartfactory.scada.esg.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.esg.dto.EsgScoreResponse;
import com.smartfactory.scada.esg.mapper.EsgMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EsgService {

	private final EsgMapper esgMapper;

	@Transactional(readOnly = true)
	public List<EsgScoreResponse> getScores(LocalDate targetMonth) {
		return esgMapper.findScores(targetMonth)
			.stream()
			.map(EsgScoreResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public EsgScoreResponse getLatestScore(Long plantId) {
		return esgMapper.findLatestByPlantId(plantId)
			.map(EsgScoreResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
	}
}
