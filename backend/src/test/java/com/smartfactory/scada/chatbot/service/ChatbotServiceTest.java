package com.smartfactory.scada.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.smartfactory.scada.esg.domain.EsgGrade;
import com.smartfactory.scada.esg.domain.EsgScore;
import com.smartfactory.scada.esg.mapper.EsgMapper;
import com.smartfactory.scada.facility.domain.Facility;
import com.smartfactory.scada.facility.domain.FacilityStatus;
import com.smartfactory.scada.facility.domain.FacilityType;
import com.smartfactory.scada.facility.mapper.FacilityMapper;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

	@Mock
	private ChatbotMapper chatbotMapper;

	@Mock
	private EnergyMapper energyMapper;

	@Mock
	private EsgMapper esgMapper;

	@Mock
	private AlarmMapper alarmMapper;

	@Mock
	private FacilityMapper facilityMapper;

	@Mock
	private OpenAiChatbotClient openAiChatbotClient;

	private ChatbotService chatbotService;

	@BeforeEach
	void setUp() {
		chatbotService = new ChatbotService(
			chatbotMapper,
			energyMapper,
			esgMapper,
			alarmMapper,
			facilityMapper,
			new ObjectMapper(),
			openAiChatbotClient
		);
	}

	@Test
	void askStoresOpenAiAnswerWhenGenerationSucceeds() {
		EnergySummary summary = energySummary();
		EsgScore score = esgScore();
		List<ChatbotMessage> recentMessages = List.of(recentMessage());
		given(energyMapper.findLatestPlantSummary(1L)).willReturn(Optional.of(summary));
		given(esgMapper.findLatestByPlantId(1L)).willReturn(Optional.of(score));
		stubOperationalContext(
			1L,
			List.of(alarm()),
			1L,
			List.of(
				facility(101L, "Press Line 1", FacilityType.PRESS, FacilityStatus.WARNING),
				facility(102L, "Press Line 2", FacilityType.PRESS, FacilityStatus.RUNNING)
			)
		);
		given(chatbotMapper.findRecent(100L, 1L, 5)).willReturn(recentMessages);
		given(openAiChatbotClient.generateAnswer(eq("status?"), anyString(), eq(recentMessages)))
			.willReturn(Optional.of("AI generated answer"));
		stubInsertId();
		given(chatbotMapper.findById(10L)).willReturn(Optional.empty());

		ChatbotMessageResponse response = chatbotService.ask(
			authenticatedUser(1L),
			new ChatbotMessageRequest(1L, " status? ")
		);

		ArgumentCaptor<ChatbotMessage> messageCaptor = ArgumentCaptor.forClass(ChatbotMessage.class);
		then(chatbotMapper).should().insert(messageCaptor.capture());
		ChatbotMessage savedMessage = messageCaptor.getValue();
		assertThat(savedMessage.getUserId()).isEqualTo(100L);
		assertThat(savedMessage.getPlantId()).isEqualTo(1L);
		assertThat(savedMessage.getQuestion()).isEqualTo("status?");
		assertThat(savedMessage.getAnswer()).isEqualTo("AI generated answer");
		assertThat(savedMessage.getReferencedData()).contains("\"plantId\":1");
		assertThat(savedMessage.getReferencedData()).contains("\"occurredAlarmCount\":1");
		assertThat(savedMessage.getReferencedData()).contains("\"recentOccurredAlarms\"");
		assertThat(savedMessage.getReferencedData()).contains("\"facilityStatusSummary\"");
		assertThat(savedMessage.getReferencedData()).contains("\"abnormalFacilities\"");
		assertThat(response.answer()).isEqualTo("AI generated answer");
	}

	@Test
	void askFallsBackToLocalSummaryWhenOpenAiReturnsEmpty() {
		given(energyMapper.findLatestPlantSummary(1L)).willReturn(Optional.of(energySummary()));
		given(esgMapper.findLatestByPlantId(1L)).willReturn(Optional.of(esgScore()));
		stubOperationalContext(
			1L,
			List.of(alarm()),
			1L,
			List.of(facility(101L, "Press Line 1", FacilityType.PRESS, FacilityStatus.WARNING))
		);
		given(chatbotMapper.findRecent(100L, 1L, 5)).willReturn(List.of());
		given(openAiChatbotClient.generateAnswer(eq("summary"), anyString(), eq(List.of())))
			.willReturn(Optional.empty());
		stubInsertId();
		given(chatbotMapper.findById(10L)).willReturn(Optional.empty());

		ChatbotMessageResponse response = chatbotService.ask(
			authenticatedUser(1L),
			new ChatbotMessageRequest(1L, "summary")
		);

		assertThat(response.answer()).contains("1234.50kWh", "1400.00kW", "AA", "91.20", "Press Line 1", "WARNING");
	}

	@Test
	void askUsesAuthenticatedUserPlantWhenRequestPlantIdIsMissing() {
		given(energyMapper.findLatestPlantSummary(2L)).willReturn(Optional.empty());
		given(esgMapper.findLatestByPlantId(2L)).willReturn(Optional.empty());
		stubOperationalContext(2L, List.of(), 0L, List.of());
		given(chatbotMapper.findRecent(100L, 2L, 5)).willReturn(List.of());
		given(openAiChatbotClient.generateAnswer(eq("summary"), anyString(), eq(List.of())))
			.willReturn(Optional.of("plant 2 answer"));
		stubInsertId();
		given(chatbotMapper.findById(10L)).willReturn(Optional.empty());

		ChatbotMessageResponse response = chatbotService.ask(
			authenticatedUser(2L),
			new ChatbotMessageRequest(null, "summary")
		);

		assertThat(response.plantId()).isEqualTo(2L);
		assertThat(response.answer()).isEqualTo("plant 2 answer");
		then(chatbotMapper).should().findRecent(100L, 2L, 5);
	}

	@Test
	void askUsesSelectionGuideAndSkipsOperationalLookupsWhenPlantIdIsMissing() {
		given(chatbotMapper.findRecent(100L, null, 5)).willReturn(List.of());
		stubInsertId();
		given(chatbotMapper.findById(10L)).willReturn(Optional.empty());

		ChatbotMessageResponse response = chatbotService.ask(
			authenticatedUser(null),
			new ChatbotMessageRequest(null, "summary")
		);

		assertThat(response.plantId()).isNull();
		assertThat(response.answer()).contains("\uc0ac\uc5c5\uc7a5\uc744 \uc120\ud0dd\ud558\uba74");
		then(energyMapper).shouldHaveNoInteractions();
		then(esgMapper).shouldHaveNoInteractions();
		then(alarmMapper).shouldHaveNoInteractions();
		then(facilityMapper).shouldHaveNoInteractions();
		then(openAiChatbotClient).shouldHaveNoInteractions();
	}

	@Test
	void askFallbackSaysNormalWhenNoOccurredAlarmsAndNoAbnormalFacilities() {
		given(energyMapper.findLatestPlantSummary(1L)).willReturn(Optional.empty());
		given(esgMapper.findLatestByPlantId(1L)).willReturn(Optional.empty());
		stubOperationalContext(
			1L,
			List.of(),
			0L,
			List.of(facility(101L, "Press Line 1", FacilityType.PRESS, FacilityStatus.RUNNING))
		);
		given(chatbotMapper.findRecent(100L, 1L, 5)).willReturn(List.of());
		given(openAiChatbotClient.generateAnswer(eq("summary"), anyString(), eq(List.of())))
			.willReturn(Optional.empty());
		stubInsertId();
		given(chatbotMapper.findById(10L)).willReturn(Optional.empty());

		ChatbotMessageResponse response = chatbotService.ask(
			authenticatedUser(1L),
			new ChatbotMessageRequest(1L, "summary")
		);

		assertThat(response.answer()).contains(
			"\ubbf8\ucc98\ub9ac \uc54c\ub78c\uc740 \uc5c6\uc2b5\ub2c8\ub2e4.",
			"\ube44\uc815\uc0c1 \uc124\ube44\ub294 \uc5c6\uc2b5\ub2c8\ub2e4."
		);
	}

	@Test
	void askThrowsAuthenticationRequiredWhenUserIsMissing() {
		assertThatThrownBy(() -> chatbotService.ask(null, new ChatbotMessageRequest(1L, "summary")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED)
			);

		then(chatbotMapper).shouldHaveNoInteractions();
		then(openAiChatbotClient).shouldHaveNoInteractions();
	}

	private void stubOperationalContext(Long plantId, List<Alarm> alarms, long occurredAlarmCount, List<Facility> facilities) {
		given(alarmMapper.findAlarms(plantId, AlarmStatus.OCCURRED, null, 5)).willReturn(alarms);
		given(alarmMapper.countOccurred(plantId)).willReturn(occurredAlarmCount);
		given(facilityMapper.findByPlantId(plantId)).willReturn(facilities);
	}

	private void stubInsertId() {
		willAnswer(invocation -> {
			ChatbotMessage message = invocation.getArgument(0);
			message.setId(10L);
			return null;
		}).given(chatbotMapper).insert(any(ChatbotMessage.class));
	}

	private AuthenticatedUser authenticatedUser(Long plantId) {
		return new AuthenticatedUser(
			100L,
			"operator@example.com",
			"operator",
			"010-0000-0000",
			"OPERATOR",
			plantId,
			"ACTIVE"
		);
	}

	private ChatbotMessage recentMessage() {
		ChatbotMessage message = new ChatbotMessage();
		message.setQuestion("previous question");
		message.setAnswer("previous answer");
		return message;
	}

	private Alarm alarm() {
		Alarm alarm = new Alarm();
		alarm.setId(1L);
		alarm.setPlantId(1L);
		alarm.setFacilityId(101L);
		alarm.setFacilityName("Press Line 1");
		alarm.setAlarmType(AlarmType.PEAK);
		alarm.setAlarmLevel(AlarmLevel.WARNING);
		alarm.setMessage("Peak power exceeded");
		alarm.setValue(new BigDecimal("1500.00"));
		alarm.setThresholdValue(new BigDecimal("1400.00"));
		alarm.setOccurredAt(LocalDateTime.of(2026, 5, 18, 10, 30));
		alarm.setStatus(AlarmStatus.OCCURRED);
		return alarm;
	}

	private Facility facility(Long id, String name, FacilityType facilityType, FacilityStatus status) {
		Facility facility = new Facility();
		facility.setId(id);
		facility.setPlantId(1L);
		facility.setName(name);
		facility.setFacilityType(facilityType);
		facility.setStatus(status);
		return facility;
	}

	private EnergySummary energySummary() {
		EnergySummary summary = new EnergySummary();
		summary.setPlantId(1L);
		summary.setElectricityKwh(new BigDecimal("1234.50"));
		summary.setPeakKw(new BigDecimal("1400.00"));
		return summary;
	}

	private EsgScore esgScore() {
		EsgScore score = new EsgScore();
		score.setPlantId(1L);
		score.setGrade(EsgGrade.AA);
		score.setTotalScore(new BigDecimal("91.20"));
		return score;
	}
}
