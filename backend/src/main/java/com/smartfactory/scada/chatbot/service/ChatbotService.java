package com.smartfactory.scada.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.chatbot.domain.ChatbotMessage;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageRequest;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageResponse;
import com.smartfactory.scada.chatbot.mapper.ChatbotMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.esg.mapper.EsgMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatbotService {

	private final ChatbotMapper chatbotMapper;
	private final EnergyMapper energyMapper;
	private final EsgMapper esgMapper;
	private final ObjectMapper objectMapper;

	@Transactional
	public ChatbotMessageResponse ask(AuthenticatedUser authenticatedUser, ChatbotMessageRequest request) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		Long plantId = request.plantId() == null ? authenticatedUser.plantId() : request.plantId();
		String referencedData = buildReferencedData(plantId);

		ChatbotMessage message = new ChatbotMessage();
		message.setUserId(authenticatedUser.userId());
		message.setPlantId(plantId);
		message.setQuestion(request.question());
		message.setAnswer(buildAnswer(plantId));
		message.setReferencedData(referencedData);
		message.setCreatedAt(LocalDateTime.now());
		message.setKey(UUID.randomUUID().toString());

		chatbotMapper.insert(message);
		return chatbotMapper.findById(message.getId())
			.map(ChatbotMessageResponse::from)
			.orElse(ChatbotMessageResponse.from(message));
	}

	@Transactional(readOnly = true)
	public List<ChatbotMessageResponse> getRecent(AuthenticatedUser authenticatedUser, Long plantId, Integer limit) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		int normalizedLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
		return chatbotMapper.findRecent(authenticatedUser.userId(), plantId, normalizedLimit)
			.stream()
			.map(ChatbotMessageResponse::from)
			.toList();
	}

	private String buildAnswer(Long plantId) {
		if (plantId == null) {
			return "사업장을 선택하면 최근 에너지 사용량, ESG 등급, 알림을 기준으로 답변할 수 있습니다.";
		}
		var latestSummary = energyMapper.findLatestPlantSummary(plantId);
		var latestScore = esgMapper.findLatestByPlantId(plantId);
		if (latestSummary.isEmpty() && latestScore.isEmpty()) {
			return "선택한 사업장의 최근 집계 데이터가 아직 없습니다.";
		}
		String energyText = latestSummary
			.map(summary -> "최근 전력 사용량은 " + summary.getElectricityKwh() + "kWh, 피크 전력은 "
				+ summary.getPeakKw() + "kW입니다.")
			.orElse("최근 에너지 집계는 없습니다.");
		String esgText = latestScore
			.map(score -> "최신 ESG 등급은 " + score.getGrade() + ", 점수는 " + score.getTotalScore() + "점입니다.")
			.orElse("최신 ESG 점수는 없습니다.");
		return energyText + " " + esgText;
	}

	private String buildReferencedData(Long plantId) {
		try {
			return objectMapper.writeValueAsString(new ReferencedData(
				plantId,
				plantId == null ? null : energyMapper.findLatestPlantSummary(plantId).orElse(null),
				plantId == null ? null : esgMapper.findLatestByPlantId(plantId).orElse(null)
			));
		}
		catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private record ReferencedData(Long plantId, Object latestEnergySummary, Object latestEsgScore) {
	}
}
