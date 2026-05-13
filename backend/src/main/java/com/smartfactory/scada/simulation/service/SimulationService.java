package com.smartfactory.scada.simulation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.esg.mapper.EsgMapper;
import com.smartfactory.scada.simulation.domain.SimulationResult;
import com.smartfactory.scada.simulation.dto.SimulationRequest;
import com.smartfactory.scada.simulation.dto.SimulationResponse;
import com.smartfactory.scada.simulation.mapper.SimulationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimulationService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

	private final SimulationMapper simulationMapper;
	private final EsgMapper esgMapper;

	@Transactional
	public SimulationResponse simulate(AuthenticatedUser authenticatedUser, SimulationRequest request) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		var baseScore = esgMapper.findLatestByPlantId(request.plantId())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
		BigDecimal expectedScore = calculateExpectedScore(baseScore.getTotalScore(), request);

		SimulationResult result = new SimulationResult();
		result.setUserId(authenticatedUser.userId());
		result.setPlantId(request.plantId());
		result.setBaseScore(baseScore.getTotalScore());
		result.setBaseGrade(baseScore.getGrade().name());
		result.setElectricityReductionRate(orZero(request.electricityReductionRate()));
		result.setGasReductionRate(orZero(request.gasReductionRate()));
		result.setWaterReductionRate(orZero(request.waterReductionRate()));
		result.setPeakReductionRate(orZero(request.peakReductionRate()));
		result.setSolarIncreaseRate(orZero(request.solarIncreaseRate()));
		result.setExpectedScore(expectedScore);
		result.setExpectedGrade(toGrade(expectedScore));
		result.setAnalysisResult(buildAnalysis(result));
		result.setCreatedAt(LocalDateTime.now());
		result.setKey(UUID.randomUUID().toString());

		simulationMapper.insert(result);
		return simulationMapper.findById(result.getId())
			.map(SimulationResponse::from)
			.orElse(SimulationResponse.from(result));
	}

	@Transactional(readOnly = true)
	public List<SimulationResponse> getRecent(AuthenticatedUser authenticatedUser, Long plantId, Integer limit) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		int normalizedLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
		return simulationMapper.findRecent(authenticatedUser.userId(), plantId, normalizedLimit)
			.stream()
			.map(SimulationResponse::from)
			.toList();
	}

	private BigDecimal calculateExpectedScore(BigDecimal baseScore, SimulationRequest request) {
		BigDecimal improvement = orZero(request.electricityReductionRate())
			.add(orZero(request.gasReductionRate()))
			.add(orZero(request.waterReductionRate()))
			.add(orZero(request.peakReductionRate()))
			.add(orZero(request.solarIncreaseRate()))
			.multiply(new BigDecimal("0.03"));
		BigDecimal expected = orZero(baseScore).add(improvement);
		if (expected.compareTo(MAX_SCORE) > 0) {
			return MAX_SCORE.setScale(2, RoundingMode.HALF_UP);
		}
		if (expected.compareTo(ZERO) < 0) {
			return ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return expected.setScale(2, RoundingMode.HALF_UP);
	}

	private String toGrade(BigDecimal score) {
		if (score.compareTo(new BigDecimal("9.0")) >= 0) return "AAA";
		if (score.compareTo(new BigDecimal("8.0")) >= 0) return "AA";
		if (score.compareTo(new BigDecimal("7.0")) >= 0) return "A";
		if (score.compareTo(new BigDecimal("6.0")) >= 0) return "BBB";
		if (score.compareTo(new BigDecimal("5.0")) >= 0) return "BB";
		if (score.compareTo(new BigDecimal("4.0")) >= 0) return "B";
		return "CCC";
	}

	private String buildAnalysis(SimulationResult result) {
		return "예상 ESG 점수는 " + result.getBaseScore() + "점에서 " + result.getExpectedScore()
			+ "점으로 변경됩니다. 예상 등급은 " + result.getExpectedGrade() + "입니다.";
	}

	private BigDecimal orZero(BigDecimal value) {
		return value == null ? ZERO : value;
	}
}
