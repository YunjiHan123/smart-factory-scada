package com.smartfactory.scada.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;

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

	private static final int OPENAI_CONTEXT_MESSAGE_LIMIT = 5;
	private static final int RECENT_MESSAGE_LIMIT_DEFAULT = 20;
	private static final int RECENT_MESSAGE_LIMIT_MAX = 100;

	private final ChatbotMapper chatbotMapper;
	private final EnergyMapper energyMapper;
	private final EsgMapper esgMapper;
	private final ObjectMapper objectMapper;
	private final OpenAiChatbotClient openAiChatbotClient;

	@Transactional
	public ChatbotMessageResponse ask(AuthenticatedUser authenticatedUser, ChatbotMessageRequest request) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		Long plantId = request.plantId() == null ? authenticatedUser.plantId() : request.plantId();
		String question = request.question().trim();
		String referencedData = buildReferencedData(plantId);
		List<ChatbotMessage> recentMessages = chatbotMapper.findRecent(
			authenticatedUser.userId(),
			plantId,
			OPENAI_CONTEXT_MESSAGE_LIMIT
		);
		String answer = openAiChatbotClient.generateAnswer(question, referencedData, recentMessages)
			.orElseGet(() -> buildFallbackAnswer(plantId));

		ChatbotMessage message = new ChatbotMessage();
		message.setUserId(authenticatedUser.userId());
		message.setPlantId(plantId);
		message.setQuestion(question);
		message.setAnswer(answer);
		message.setReferencedData(referencedData);
		message.setCreatedAt(LocalDateTime.now());

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

		int normalizedLimit = limit == null || limit <= 0
			? RECENT_MESSAGE_LIMIT_DEFAULT
			: Math.min(limit, RECENT_MESSAGE_LIMIT_MAX);
		return chatbotMapper.findRecent(authenticatedUser.userId(), plantId, normalizedLimit)
			.stream()
			.map(ChatbotMessageResponse::from)
			.toList();
	}

	private String buildFallbackAnswer(Long plantId) {
		if (plantId == null) {
			return "\uc0ac\uc5c5\uc7a5\uc744 \uc120\ud0dd\ud558\uba74 \ucd5c\uadfc \uc5d0\ub108\uc9c0 \uc0ac\uc6a9\ub7c9, ESG \ub4f1\uae09, \uc54c\ub78c \uc815\ubcf4\ub97c \uae30\uc900\uc73c\ub85c \ub2f5\ubcc0\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.";
		}

		var latestSummary = energyMapper.findLatestPlantSummary(plantId);
		var latestScore = esgMapper.findLatestByPlantId(plantId);
		if (latestSummary.isEmpty() && latestScore.isEmpty()) {
			return "\uc120\ud0dd\ud55c \uc0ac\uc5c5\uc7a5\uc758 \ucd5c\uadfc \uc9d1\uacc4 \ub370\uc774\ud130\uac00 \uc544\uc9c1 \uc5c6\uc2b5\ub2c8\ub2e4. \uc5d0\ub108\uc9c0 \uce21\uc815 \ub370\uc774\ud130\uac00 \uc218\uc9d1\ub41c \ub4a4 \ub2e4\uc2dc \ud655\uc778\ud574 \uc8fc\uc138\uc694.";
		}

		String energyText = latestSummary
			.map(summary -> "\ucd5c\uadfc \uc804\ub825 \uc0ac\uc6a9\ub7c9\uc740 " + summary.getElectricityKwh()
				+ "kWh, \ud53c\ud06c \uc804\ub825\uc740 " + summary.getPeakKw() + "kW\uc785\ub2c8\ub2e4.")
			.orElse("\ucd5c\uadfc \uc5d0\ub108\uc9c0 \uc9d1\uacc4 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
		String esgText = latestScore
			.map(score -> "\ucd5c\uc2e0 ESG \ub4f1\uae09\uc740 " + score.getGrade() + ", \ucd1d\uc810\uc740 "
				+ score.getTotalScore() + "\uc810\uc785\ub2c8\ub2e4.")
			.orElse("\ucd5c\uc2e0 ESG \uc810\uc218 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
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
