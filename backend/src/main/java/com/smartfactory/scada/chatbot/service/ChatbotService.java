package com.smartfactory.scada.chatbot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.alarm.domain.Alarm;
import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.domain.AlarmType;
import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.chatbot.domain.ChatbotMessage;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageRequest;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageResponse;
import com.smartfactory.scada.chatbot.mapper.ChatbotMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.esg.domain.EsgScore;
import com.smartfactory.scada.esg.mapper.EsgMapper;
import com.smartfactory.scada.facility.domain.Facility;
import com.smartfactory.scada.facility.domain.FacilityStatus;
import com.smartfactory.scada.facility.domain.FacilityType;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatbotService {

	private static final int OPENAI_CONTEXT_MESSAGE_LIMIT = 5;
	private static final int RECENT_ALARM_LIMIT = 5;
	private static final int RECENT_MESSAGE_LIMIT_DEFAULT = 20;
	private static final int RECENT_MESSAGE_LIMIT_MAX = 100;

	private final ChatbotMapper chatbotMapper;
	private final EnergyMapper energyMapper;
	private final EsgMapper esgMapper;
	private final AlarmMapper alarmMapper;
	private final FacilityMapper facilityMapper;
	private final ObjectMapper objectMapper;
	private final OpenAiChatbotClient openAiChatbotClient;

	@Transactional
	public ChatbotMessageResponse ask(AuthenticatedUser authenticatedUser, ChatbotMessageRequest request) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		Long plantId = request.plantId() == null ? authenticatedUser.plantId() : request.plantId();
		String question = request.question().trim();
		OperationalContext operationalContext = buildOperationalContext(plantId);
		String referencedData = serializeReferencedData(operationalContext);
		List<ChatbotMessage> recentMessages = chatbotMapper.findRecent(
			authenticatedUser.userId(),
			plantId,
			OPENAI_CONTEXT_MESSAGE_LIMIT
		);
		String answer = plantId == null
			? buildFallbackAnswer(operationalContext)
			: openAiChatbotClient.generateAnswer(question, referencedData, recentMessages)
				.orElseGet(() -> buildFallbackAnswer(operationalContext));

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

	private String buildFallbackAnswer(OperationalContext operationalContext) {
		if (operationalContext.plantId() == null) {
			return "\uc0ac\uc5c5\uc7a5\uc744 \uc120\ud0dd\ud558\uba74 \ucd5c\uadfc \uc5d0\ub108\uc9c0 \uc0ac\uc6a9\ub7c9, ESG \ub4f1\uae09, \uc54c\ub78c \uc815\ubcf4\ub97c \uae30\uc900\uc73c\ub85c \ub2f5\ubcc0\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.";
		}

		String energyText = operationalContext.latestEnergySummary() == null
			? "\ucd5c\uadfc \uc5d0\ub108\uc9c0 \uc9d1\uacc4 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4."
			: "\ucd5c\uadfc \uc804\ub825 \uc0ac\uc6a9\ub7c9\uc740 "
				+ operationalContext.latestEnergySummary().getElectricityKwh()
				+ "kWh, \ud53c\ud06c \uc804\ub825\uc740 "
				+ operationalContext.latestEnergySummary().getPeakKw() + "kW\uc785\ub2c8\ub2e4.";
		String esgText = operationalContext.latestEsgScore() == null
			? "\ucd5c\uc2e0 ESG \uc810\uc218 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4."
			: "\ucd5c\uc2e0 ESG \ub4f1\uae09\uc740 " + operationalContext.latestEsgScore().getGrade()
				+ ", \ucd1d\uc810\uc740 " + operationalContext.latestEsgScore().getTotalScore() + "\uc810\uc785\ub2c8\ub2e4.";
		String alarmText = buildAlarmFallbackText(operationalContext);
		String facilityText = buildFacilityFallbackText(operationalContext.facilityStatusSummary());
		return energyText + " " + esgText + " " + alarmText + " " + facilityText;
	}

	private String buildAlarmFallbackText(OperationalContext operationalContext) {
		if (operationalContext.occurredAlarmCount() == 0) {
			return "\ubbf8\ucc98\ub9ac \uc54c\ub78c\uc740 \uc5c6\uc2b5\ub2c8\ub2e4.";
		}

		return "\ubbf8\ucc98\ub9ac \uc54c\ub78c\uc740 " + operationalContext.occurredAlarmCount()
			+ "\uac74\uc774\uba70, \ucd5c\uadfc \uc54c\ub78c\uc740 "
			+ summarizeRecentAlarm(operationalContext.recentOccurredAlarms()) + "\uc785\ub2c8\ub2e4.";
	}

	private String buildFacilityFallbackText(FacilityStatusSummary facilityStatusSummary) {
		if (facilityStatusSummary.abnormalFacilities().isEmpty()) {
			return "\ube44\uc815\uc0c1 \uc124\ube44\ub294 \uc5c6\uc2b5\ub2c8\ub2e4.";
		}

		return "\ube44\uc815\uc0c1 \uc124\ube44\ub294 " + facilityStatusSummary.abnormalFacilities().size()
			+ "\uac1c\uc774\uba70, " + summarizeAbnormalFacilities(facilityStatusSummary.abnormalFacilities())
			+ " \uc0c1\ud0dc\ub97c \ud655\uc778\ud558\uc138\uc694.";
	}

	private String summarizeRecentAlarm(List<AlarmContext> recentOccurredAlarms) {
		if (recentOccurredAlarms.isEmpty()) {
			return "\ucd5c\uadfc \uc54c\ub78c \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.";
		}

		AlarmContext alarm = recentOccurredAlarms.get(0);
		String facilityName = alarm.facilityName() == null ? "-" : alarm.facilityName();
		return facilityName + " " + alarm.alarmLevel() + " " + alarm.message();
	}

	private String summarizeAbnormalFacilities(List<FacilityStatusItem> abnormalFacilities) {
		return abnormalFacilities.stream()
			.limit(3)
			.map(facility -> facility.name() + "(" + facility.status() + ")")
			.collect(Collectors.joining(", "));
	}

	private OperationalContext buildOperationalContext(Long plantId) {
		if (plantId == null) {
			return new OperationalContext(null, null, null, 0L, List.of(), FacilityStatusSummary.empty());
		}

		List<AlarmContext> recentOccurredAlarms = alarmMapper.findAlarms(
				plantId,
				AlarmStatus.OCCURRED,
				null,
				RECENT_ALARM_LIMIT
			)
			.stream()
			.map(AlarmContext::from)
			.toList();
		List<Facility> facilities = facilityMapper.findByPlantId(plantId);

		return new OperationalContext(
			plantId,
			energyMapper.findLatestPlantSummary(plantId).orElse(null),
			esgMapper.findLatestByPlantId(plantId).orElse(null),
			alarmMapper.countOccurred(plantId),
			recentOccurredAlarms,
			summarizeFacilities(facilities)
		);
	}

	private FacilityStatusSummary summarizeFacilities(List<Facility> facilities) {
		Map<String, Long> statusCounts = new LinkedHashMap<>();
		for (FacilityStatus status : FacilityStatus.values()) {
			statusCounts.put(status.name(), 0L);
		}

		List<FacilityStatusItem> abnormalFacilities = new ArrayList<>();
		for (Facility facility : facilities) {
			FacilityStatus status = facility.getStatus();
			statusCounts.merge(status == null ? "UNKNOWN" : status.name(), 1L, Long::sum);
			if (status != FacilityStatus.RUNNING) {
				abnormalFacilities.add(FacilityStatusItem.from(facility));
			}
		}

		return new FacilityStatusSummary(facilities.size(), statusCounts, abnormalFacilities);
	}

	private String serializeReferencedData(OperationalContext operationalContext) {
		try {
			return objectMapper.writeValueAsString(ReferencedData.from(operationalContext));
		}
		catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private record OperationalContext(
		Long plantId,
		EnergySummary latestEnergySummary,
		EsgScore latestEsgScore,
		long occurredAlarmCount,
		List<AlarmContext> recentOccurredAlarms,
		FacilityStatusSummary facilityStatusSummary
	) {
	}

	private record ReferencedData(
		Long plantId,
		Object latestEnergySummary,
		Object latestEsgScore,
		long occurredAlarmCount,
		List<AlarmContext> recentOccurredAlarms,
		FacilityStatusSummary facilityStatusSummary
	) {

		private static ReferencedData from(OperationalContext operationalContext) {
			return new ReferencedData(
				operationalContext.plantId(),
				operationalContext.latestEnergySummary(),
				operationalContext.latestEsgScore(),
				operationalContext.occurredAlarmCount(),
				operationalContext.recentOccurredAlarms(),
				operationalContext.facilityStatusSummary()
			);
		}
	}

	private record AlarmContext(
		Long id,
		Long facilityId,
		String facilityName,
		AlarmType alarmType,
		AlarmLevel alarmLevel,
		String message,
		BigDecimal value,
		BigDecimal thresholdValue,
		String occurredAt,
		AlarmStatus status
	) {

		private static AlarmContext from(Alarm alarm) {
			return new AlarmContext(
				alarm.getId(),
				alarm.getFacilityId(),
				alarm.getFacilityName(),
				alarm.getAlarmType(),
				alarm.getAlarmLevel(),
				alarm.getMessage(),
				alarm.getValue(),
				alarm.getThresholdValue(),
				alarm.getOccurredAt() == null ? null : alarm.getOccurredAt().toString(),
				alarm.getStatus()
			);
		}
	}

	private record FacilityStatusSummary(
		int totalCount,
		Map<String, Long> statusCounts,
		List<FacilityStatusItem> abnormalFacilities
	) {

		private static FacilityStatusSummary empty() {
			return new FacilityStatusSummary(0, Map.of(), List.of());
		}
	}

	private record FacilityStatusItem(
		Long id,
		String name,
		FacilityType facilityType,
		FacilityStatus status
	) {

		private static FacilityStatusItem from(Facility facility) {
			return new FacilityStatusItem(
				facility.getId(),
				facility.getName(),
				facility.getFacilityType(),
				facility.getStatus()
			);
		}
	}
}
