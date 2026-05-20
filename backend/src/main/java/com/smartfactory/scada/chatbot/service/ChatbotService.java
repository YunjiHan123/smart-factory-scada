package com.smartfactory.scada.chatbot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.smartfactory.scada.chatbot.dto.ChatbotAiResponse;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageRequest;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageResponse;
import com.smartfactory.scada.chatbot.mapper.ChatbotMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.ElectricityBillDataStatus;
import com.smartfactory.scada.energy.domain.PeakPowerPeriod;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonResponse;
import com.smartfactory.scada.energy.dto.ElectricityBillComparisonRowResponse;
import com.smartfactory.scada.energy.dto.EnergyFacilityLineUsageResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.energy.service.ElectricityBillComparisonService;
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
	private static final int TREND_DAYS = 7;
	private static final int FACILITY_USAGE_TOP_LIMIT = 5;
	private static final int RECENT_MESSAGE_LIMIT_DEFAULT = 20;
	private static final int RECENT_MESSAGE_LIMIT_MAX = 100;

	private final ChatbotMapper chatbotMapper;
	private final EnergyMapper energyMapper;
	private final EsgMapper esgMapper;
	private final AlarmMapper alarmMapper;
	private final FacilityMapper facilityMapper;
	private final ObjectMapper objectMapper;
	private final OpenAiChatbotClient openAiChatbotClient;
	private final ElectricityBillComparisonService electricityBillComparisonService;

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
		ChatbotAiResponse generatedResponse = plantId == null
			? ChatbotAiResponse.answerOnly(buildFallbackAnswer(question, operationalContext))
			: openAiChatbotClient.generateResponse(question, referencedData, recentMessages)
				.orElseGet(() -> ChatbotAiResponse.answerOnly(buildFallbackAnswer(question, operationalContext)));
		generatedResponse = generatedResponse.withChartSpec(buildChartSpec(question, operationalContext));

		ChatbotMessage message = new ChatbotMessage();
		message.setUserId(authenticatedUser.userId());
		message.setPlantId(plantId);
		message.setQuestion(question);
		message.setAnswer(generatedResponse.answer());
		message.setReferencedData(referencedData);
		message.setChartSpec(generatedResponse.chartSpec());
		message.setImageDataUrl(generatedResponse.imageDataUrl());
		message.setExternalSources(generatedResponse.externalSources());
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

	@Transactional
	public void deleteMessage(AuthenticatedUser authenticatedUser, Long messageId) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}
		if (messageId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		int deletedCount = chatbotMapper.deleteByIdAndUserId(messageId, authenticatedUser.userId());
		if (deletedCount == 0) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}
	}

	@Transactional
	public void deleteMessagesByPlant(AuthenticatedUser authenticatedUser, Long plantId) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}
		if (plantId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}

		chatbotMapper.deleteByUserIdAndPlantId(authenticatedUser.userId(), plantId);
	}

	private String buildFallbackAnswer(String question, OperationalContext operationalContext) {
		if (operationalContext.plantId() == null) {
			return "\uc0ac\uc5c5\uc7a5\uc744 \uc120\ud0dd\ud558\uba74 \ucd5c\uadfc \uc5d0\ub108\uc9c0 \uc0ac\uc6a9\ub7c9, ESG \ub4f1\uae09, \uc54c\ub78c \uc815\ubcf4\ub97c \uae30\uc900\uc73c\ub85c \ub2f5\ubcc0\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.";
		}
		if (isElectricityBillQuestion(question)) {
			return buildElectricityBillFallbackText(operationalContext.electricityBillComparison());
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
		String trendText = buildTrendFallbackText(operationalContext.trendInsights());
		String facilityLineText = buildFacilityLineFallbackText(operationalContext.facilityLineUsageSummary());
		return energyText + " " + esgText + " " + alarmText + " " + facilityText + " " + trendText + " " + facilityLineText;
	}

	private String buildElectricityBillFallbackText(ElectricityBillComparisonResponse comparison) {
		if (comparison == null || comparison.dataStatus() == ElectricityBillDataStatus.EMPTY || comparison.comparisons().isEmpty()) {
			return "선택한 사업장의 월 단위 전기요금 비교 데이터가 없습니다. 피크 전력과 전력량 누적 데이터가 쌓이면 고압A 선택II 기준 추정 전기요금과 요금제별 절감 가능성을 계산할 수 있습니다.";
		}

		String savingText = comparison.estimatedSavingKrw().compareTo(BigDecimal.ZERO) > 0
			? "예상 절감액은 " + formatMoney(comparison.estimatedSavingKrw()) + "원(" + formatDecimal(comparison.estimatedSavingRate()) + "%)입니다."
			: "현재 기준 요금제가 최저 또는 같은 수준이라 예상 절감액은 0원입니다.";
		return comparison.periodFrom() + "~" + comparison.periodTo() + " 기준 "
			+ comparison.baseTariffName() + " 추정 합계는 " + formatMoney(comparison.baseEstimatedTotalKrw()) + "원입니다. "
			+ "가장 낮은 요금제는 " + comparison.bestTariffName() + "이며 추정 합계는 "
			+ formatMoney(comparison.bestEstimatedTotalKrw()) + "원입니다. "
			+ savingText + " 이 값은 부가세, 기후환경요금, 연료비조정액 등을 제외한 추정치입니다. "
			+ "다음 점검은 산정 피크 " + formatDecimal(comparison.billingDemandKw()) + "kW와 최대부하 시간대 사용량을 확인하세요.";
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

	private String buildTrendFallbackText(TrendInsights trendInsights) {
		if (trendInsights.latestDate() == null) {
			return "\ucd5c\uadfc 7\uc77c \ucd94\uc138 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.";
		}

		return "\ucd5c\uadfc 7\uc77c \uc804\ub825 \ud3c9\uade0\uc740 "
			+ formatDecimal(trendInsights.sevenDayAverageElectricityKwh())
			+ "kWh, \uc804\uc77c \ub300\ube44 "
			+ formatDecimal(trendInsights.latestVsPreviousRate())
			+ "%\uc785\ub2c8\ub2e4."
			+ (trendInsights.maxPeakDate() == null
				? ""
				: ", \ucd5c\ub300 \ud53c\ud06c\uc77c\uc740 " + trendInsights.maxPeakDate()
					+ " (" + formatDecimal(trendInsights.maxPeakKw()) + "kW)\uc785\ub2c8\ub2e4.");
	}

	private String buildFacilityLineFallbackText(FacilityLineUsageSummary facilityLineUsageSummary) {
		if (facilityLineUsageSummary.topFacilities().isEmpty()) {
			return "\ub77c\uc778\ubcc4 \uc0ac\uc6a9\ub7c9 \ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.";
		}

		return "\uc0ac\uc6a9\ub7c9 \uc0c1\uc704 \uc124\ube44\ub294 "
			+ facilityLineUsageSummary.topFacilities()
				.stream()
				.map(facility -> facility.facilityName() + "(" + formatDecimal(facility.todayUsageKwh()) + "kWh)")
				.collect(Collectors.joining(", "))
			+ "\uc785\ub2c8\ub2e4.";
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
			return new OperationalContext(
				null,
				null,
				null,
				null,
				0L,
				List.of(),
				FacilityStatusSummary.empty(),
				List.of(),
				TrendInsights.empty(),
				FacilityLineUsageSummary.empty(),
				null
			);
		}

		EnergySummary latestEnergySummary = energyMapper.findLatestPlantSummary(plantId).orElse(null);
		LocalDate referenceDate = latestEnergySummary == null || latestEnergySummary.getSummaryAt() == null
			? LocalDate.now()
			: latestEnergySummary.getSummaryAt().toLocalDate();
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
		List<DailyEnergyTrendPoint> dailyEnergyTrend = buildDailyEnergyTrend(plantId, referenceDate);
		ElectricityBillComparisonResponse electricityBillComparison =
			electricityBillComparisonService.compare(plantId, referenceDate, PeakPowerPeriod.MONTH, null);

		return new OperationalContext(
			plantId,
			referenceDate,
			latestEnergySummary,
			esgMapper.findLatestByPlantId(plantId).orElse(null),
			alarmMapper.countOccurred(plantId),
			recentOccurredAlarms,
			summarizeFacilities(facilities),
			dailyEnergyTrend,
			buildTrendInsights(dailyEnergyTrend),
			buildFacilityLineUsageSummary(plantId, facilities, referenceDate),
			electricityBillComparison
		);
	}

	private List<DailyEnergyTrendPoint> buildDailyEnergyTrend(Long plantId, LocalDate referenceDate) {
		LocalDate fromDate = referenceDate.minusDays(TREND_DAYS - 1L);
		return energyMapper.findSummaries(
				plantId,
				null,
				SummaryType.DAILY,
				fromDate.atStartOfDay(),
				referenceDate.plusDays(1).atStartOfDay()
			)
			.stream()
			.filter(summary -> summary.getSummaryAt() != null)
			.map(DailyEnergyTrendPoint::from)
			.sorted(Comparator.comparing(DailyEnergyTrendPoint::summaryDate))
			.toList();
	}

	private TrendInsights buildTrendInsights(List<DailyEnergyTrendPoint> dailyEnergyTrend) {
		if (dailyEnergyTrend.isEmpty()) {
			return TrendInsights.empty();
		}

		DailyEnergyTrendPoint latest = dailyEnergyTrend.get(dailyEnergyTrend.size() - 1);
		DailyEnergyTrendPoint previous = dailyEnergyTrend.size() < 2
			? null
			: dailyEnergyTrend.get(dailyEnergyTrend.size() - 2);
		DailyEnergyTrendPoint maxPeak = dailyEnergyTrend.stream()
			.max(Comparator.comparing(DailyEnergyTrendPoint::peakKw))
			.orElse(latest);

		return new TrendInsights(
			latest.summaryDate(),
			latest.electricityKwh(),
			previous == null ? null : previous.summaryDate(),
			previous == null ? BigDecimal.ZERO : previous.electricityKwh(),
			changeRate(latest.electricityKwh(), previous == null ? BigDecimal.ZERO : previous.electricityKwh()),
			averageElectricity(dailyEnergyTrend),
			maxPeak.summaryDate(),
			maxPeak.peakKw()
		);
	}

	private FacilityLineUsageSummary buildFacilityLineUsageSummary(
		Long plantId,
		List<Facility> facilities,
		LocalDate referenceDate
	) {
		List<FacilityTypeUsageSummary> facilityTypeSummaries = new ArrayList<>();
		List<FacilityUsageItem> allUsages = new ArrayList<>();

		List<FacilityType> facilityTypes = facilities.stream()
			.map(Facility::getFacilityType)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		for (FacilityType facilityType : facilityTypes) {
			LocalDate usageDate = energyMapper.findFacilityLineSummaryDate(plantId, facilityType, referenceDate)
				.or(() -> energyMapper.findLatestFacilityLineSummaryDate(plantId, facilityType))
				.orElse(referenceDate);
			List<EnergyFacilityLineUsageResponse> usages = energyMapper.findFacilityLineUsages(
				plantId,
				facilityType,
				"ELECTRICITY",
				usageDate,
				usageDate.minusDays(1),
				usageDate.withDayOfMonth(1),
				usageDate.plusMonths(1).withDayOfMonth(1),
				usageDate.atStartOfDay(),
				usageDate.plusDays(1).atStartOfDay()
			);
			List<FacilityUsageItem> usageItems = usages.stream()
				.map(FacilityUsageItem::from)
				.toList();
			facilityTypeSummaries.add(summarizeFacilityType(facilityType, usageDate, usageItems));
			allUsages.addAll(usageItems);
		}

		List<FacilityUsageItem> topFacilities = allUsages.stream()
			.sorted(Comparator.comparing(FacilityUsageItem::todayUsageKwh).reversed())
			.limit(FACILITY_USAGE_TOP_LIMIT)
			.toList();
		return new FacilityLineUsageSummary(facilityTypeSummaries, topFacilities);
	}

	private FacilityTypeUsageSummary summarizeFacilityType(
		FacilityType facilityType,
		LocalDate usageDate,
		List<FacilityUsageItem> usages
	) {
		BigDecimal totalTodayUsageKwh = usages.stream()
			.map(FacilityUsageItem::todayUsageKwh)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		FacilityUsageItem topFacility = usages.stream()
			.max(Comparator.comparing(FacilityUsageItem::todayUsageKwh))
			.orElse(null);

		return new FacilityTypeUsageSummary(
			facilityType,
			usageDate.toString(),
			usages.size(),
			totalTodayUsageKwh,
			average(totalTodayUsageKwh, usages.size()),
			topFacility == null ? null : topFacility.facilityName(),
			topFacility == null ? BigDecimal.ZERO : topFacility.todayUsageKwh()
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

	private BigDecimal averageElectricity(List<DailyEnergyTrendPoint> dailyEnergyTrend) {
		BigDecimal total = dailyEnergyTrend.stream()
			.map(DailyEnergyTrendPoint::electricityKwh)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return average(total, dailyEnergyTrend.size());
	}

	private BigDecimal average(BigDecimal total, int count) {
		if (count <= 0) {
			return BigDecimal.ZERO;
		}
		return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
	}

	private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
		if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return zeroIfNull(current).subtract(previous)
			.multiply(BigDecimal.valueOf(100))
			.divide(previous, 1, RoundingMode.HALF_UP);
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String formatDecimal(BigDecimal value) {
		return zeroIfNull(value).toPlainString();
	}

	private String formatMoney(BigDecimal value) {
		return zeroIfNull(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
	}

	private String serializeReferencedData(OperationalContext operationalContext) {
		try {
			return objectMapper.writeValueAsString(ReferencedData.from(operationalContext));
		}
		catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private String buildChartSpec(String question, OperationalContext operationalContext) {
		if (operationalContext.plantId() == null || !wantsChart(question)) {
			return null;
		}

		if (isElectricityBillQuestion(question)) {
			Map<String, Object> billChart = buildElectricityBillComparisonChart(operationalContext.electricityBillComparison());
			if (billChart == null) {
				return null;
			}
			try {
				return objectMapper.writeValueAsString(billChart);
			}
			catch (JsonProcessingException exception) {
				return null;
			}
		}

		Map<String, Object> chart = questionContains(question, "설비", "라인", "상위")
			? buildFacilityUsageChart(operationalContext.facilityLineUsageSummary())
			: questionContains(question, "비중", "구성", "믹스", "태양광", "가스", "용수")
				? buildEnergyMixChart(operationalContext.latestEnergySummary())
				: buildDailyTrendChart(operationalContext.dailyEnergyTrend());
		if (chart == null) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(chart);
		}
		catch (JsonProcessingException exception) {
			return null;
		}
	}

	private boolean wantsChart(String question) {
		return questionContains(question, "그래프", "차트", "시각화", "그려", "그려줘", "추이", "비교");
	}

	private boolean isElectricityBillQuestion(String question) {
		return questionContains(question, "전기요금", "전기세", "요금제", "절감", "비용", "요금", "원");
	}

	private boolean questionContains(String question, String... keywords) {
		String normalizedQuestion = question == null ? "" : question;
		for (String keyword : keywords) {
			if (normalizedQuestion.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private Map<String, Object> buildDailyTrendChart(List<DailyEnergyTrendPoint> dailyEnergyTrend) {
		if (dailyEnergyTrend == null || dailyEnergyTrend.isEmpty()) {
			return null;
		}
		return Map.of(
			"type", "line",
			"title", "최근 7일 에너지 사용량 추이",
			"unit", "사용량",
			"labels", dailyEnergyTrend.stream().map(DailyEnergyTrendPoint::summaryDate).toList(),
			"series", List.of(
				chartSeries("전기(kWh)", "#0f6fff", dailyEnergyTrend.stream().map(DailyEnergyTrendPoint::electricityKwh).toList()),
				chartSeries("가스(m3)", "#ff8a00", dailyEnergyTrend.stream().map(DailyEnergyTrendPoint::gasM3).toList()),
				chartSeries("용수(ton)", "#14bfd4", dailyEnergyTrend.stream().map(DailyEnergyTrendPoint::waterTon).toList()),
				chartSeries("태양광(kWh)", "#45c742", dailyEnergyTrend.stream().map(DailyEnergyTrendPoint::solarKwh).toList())
			)
		);
	}

	private Map<String, Object> buildFacilityUsageChart(FacilityLineUsageSummary facilityLineUsageSummary) {
		if (facilityLineUsageSummary == null || facilityLineUsageSummary.topFacilities().isEmpty()) {
			return null;
		}
		List<FacilityUsageItem> topFacilities = facilityLineUsageSummary.topFacilities();
		return Map.of(
			"type", "bar",
			"title", "설비별 전기 사용량 상위",
			"unit", "kWh",
			"labels", topFacilities.stream().map(FacilityUsageItem::facilityName).toList(),
			"series", List.of(chartSeries(
				"금일 사용량",
				"#0f6fff",
				topFacilities.stream().map(FacilityUsageItem::todayUsageKwh).toList()
			))
		);
	}

	private Map<String, Object> buildEnergyMixChart(EnergySummary latestEnergySummary) {
		if (latestEnergySummary == null) {
			return null;
		}
		return Map.of(
			"type", "bar",
			"title", "최신 에너지 사용량 구성",
			"unit", "현재 집계값",
			"labels", List.of("전기(kWh)", "가스(m3)", "용수(ton)", "태양광(kWh)"),
			"series", List.of(chartSeries(
				"에너지",
				"#0f6fff",
				List.of(
					zeroIfNull(latestEnergySummary.getElectricityKwh()),
					zeroIfNull(latestEnergySummary.getGasM3()),
					zeroIfNull(latestEnergySummary.getWaterTon()),
					zeroIfNull(latestEnergySummary.getSolarKwh())
				)
			))
		);
	}

	private Map<String, Object> buildElectricityBillComparisonChart(ElectricityBillComparisonResponse comparison) {
		if (comparison == null || comparison.comparisons().isEmpty()) {
			return null;
		}
		return Map.of(
			"type", "bar",
			"title", "요금제별 추정 전기요금 비교",
			"unit", "원",
			"labels", comparison.comparisons().stream().map(ElectricityBillComparisonRowResponse::tariffName).toList(),
			"series", List.of(chartSeries(
				"추정 합계",
				"#0f6fff",
				comparison.comparisons().stream().map(ElectricityBillComparisonRowResponse::estimatedTotalKrw).toList()
			))
		);
	}

	private Map<String, Object> chartSeries(String name, String color, List<BigDecimal> values) {
		return Map.of(
			"name", name,
			"color", color,
			"values", values.stream().map(this::zeroIfNull).toList()
		);
	}

	private record OperationalContext(
		Long plantId,
		LocalDate referenceDate,
		EnergySummary latestEnergySummary,
		EsgScore latestEsgScore,
		long occurredAlarmCount,
		List<AlarmContext> recentOccurredAlarms,
		FacilityStatusSummary facilityStatusSummary,
		List<DailyEnergyTrendPoint> dailyEnergyTrend,
		TrendInsights trendInsights,
		FacilityLineUsageSummary facilityLineUsageSummary,
		ElectricityBillComparisonResponse electricityBillComparison
	) {
	}

	private record ReferencedData(
		Long plantId,
		String referenceDate,
		EnergySummaryContext latestEnergySummary,
		EsgScoreContext latestEsgScore,
		long occurredAlarmCount,
		List<AlarmContext> recentOccurredAlarms,
		FacilityStatusSummary facilityStatusSummary,
		List<DailyEnergyTrendPoint> dailyEnergyTrend,
		TrendInsights trendInsights,
		FacilityLineUsageSummary facilityLineUsageSummary,
		ElectricityBillComparisonResponse electricityBillComparison
	) {

		private static ReferencedData from(OperationalContext operationalContext) {
			return new ReferencedData(
				operationalContext.plantId(),
				operationalContext.referenceDate() == null ? null : operationalContext.referenceDate().toString(),
				EnergySummaryContext.from(operationalContext.latestEnergySummary()),
				EsgScoreContext.from(operationalContext.latestEsgScore()),
				operationalContext.occurredAlarmCount(),
				operationalContext.recentOccurredAlarms(),
				operationalContext.facilityStatusSummary(),
				operationalContext.dailyEnergyTrend(),
				operationalContext.trendInsights(),
				operationalContext.facilityLineUsageSummary(),
				operationalContext.electricityBillComparison()
			);
		}
	}

	private record EnergySummaryContext(
		String summaryAt,
		BigDecimal electricityKwh,
		BigDecimal gasM3,
		BigDecimal waterTon,
		BigDecimal solarKwh,
		BigDecimal peakKw,
		BigDecimal carbonEmission
	) {

		private static EnergySummaryContext from(EnergySummary summary) {
			if (summary == null) {
				return null;
			}
			return new EnergySummaryContext(
				summary.getSummaryAt() == null ? null : summary.getSummaryAt().toString(),
				summary.getElectricityKwh(),
				summary.getGasM3(),
				summary.getWaterTon(),
				summary.getSolarKwh(),
				summary.getPeakKw(),
				summary.getCarbonEmission()
			);
		}
	}

	private record EsgScoreContext(
		String targetMonth,
		BigDecimal totalScore,
		String grade
	) {

		private static EsgScoreContext from(EsgScore score) {
			if (score == null) {
				return null;
			}
			return new EsgScoreContext(
				score.getTargetMonth() == null ? null : score.getTargetMonth().toString(),
				score.getTotalScore(),
				score.getGrade() == null ? null : score.getGrade().name()
			);
		}
	}

	private record DailyEnergyTrendPoint(
		String summaryDate,
		BigDecimal electricityKwh,
		BigDecimal gasM3,
		BigDecimal waterTon,
		BigDecimal solarKwh,
		BigDecimal peakKw,
		BigDecimal carbonEmission
	) {

		private static DailyEnergyTrendPoint from(EnergySummary summary) {
			return new DailyEnergyTrendPoint(
				summary.getSummaryAt().toLocalDate().toString(),
				zeroIfNullStatic(summary.getElectricityKwh()),
				zeroIfNullStatic(summary.getGasM3()),
				zeroIfNullStatic(summary.getWaterTon()),
				zeroIfNullStatic(summary.getSolarKwh()),
				zeroIfNullStatic(summary.getPeakKw()),
				zeroIfNullStatic(summary.getCarbonEmission())
			);
		}
	}

	private record TrendInsights(
		String latestDate,
		BigDecimal latestElectricityKwh,
		String previousDate,
		BigDecimal previousElectricityKwh,
		BigDecimal latestVsPreviousRate,
		BigDecimal sevenDayAverageElectricityKwh,
		String maxPeakDate,
		BigDecimal maxPeakKw
	) {

		private static TrendInsights empty() {
			return new TrendInsights(null, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO);
		}
	}

	private record FacilityLineUsageSummary(
		List<FacilityTypeUsageSummary> facilityTypeSummaries,
		List<FacilityUsageItem> topFacilities
	) {

		private static FacilityLineUsageSummary empty() {
			return new FacilityLineUsageSummary(List.of(), List.of());
		}
	}

	private record FacilityTypeUsageSummary(
		FacilityType facilityType,
		String usageDate,
		int facilityCount,
		BigDecimal totalTodayUsageKwh,
		BigDecimal averageTodayUsageKwh,
		String topFacilityName,
		BigDecimal topFacilityUsageKwh
	) {
	}

	private record FacilityUsageItem(
		Long facilityId,
		String facilityName,
		FacilityType facilityType,
		FacilityStatus facilityStatus,
		String usageDate,
		BigDecimal todayUsageKwh,
		BigDecimal todayVsYesterdayRate,
		BigDecimal todayVsMonthlyAverageRate,
		String latestMeasuredAt
	) {

		private static FacilityUsageItem from(EnergyFacilityLineUsageResponse usage) {
			return new FacilityUsageItem(
				usage.getFacilityId(),
				usage.getFacilityName(),
				usage.getFacilityType(),
				usage.getFacilityStatus(),
				usage.getUsageDate() == null ? null : usage.getUsageDate().toString(),
				zeroIfNullStatic(usage.getTodayUsageKwh()),
				zeroIfNullStatic(usage.getTodayVsYesterdayRate()),
				zeroIfNullStatic(usage.getTodayVsMonthlyAverageRate()),
				usage.getLatestMeasuredAt() == null ? null : usage.getLatestMeasuredAt().toString()
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

	private static BigDecimal zeroIfNullStatic(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
