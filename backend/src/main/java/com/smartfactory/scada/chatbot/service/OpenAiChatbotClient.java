package com.smartfactory.scada.chatbot.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.chatbot.config.OpenAiProperties;
import com.smartfactory.scada.chatbot.domain.ChatbotMessage;
import com.smartfactory.scada.chatbot.dto.ChatbotAiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenAiChatbotClient {

	private static final String INSTRUCTIONS = """
		You are the chatbot for an energy integrated control system used by Hyundai and Kia plants.
		Answer only the user's question in Korean, with a concise and useful response for plant operations.
		Do not add unrelated alarm information, anomaly details, operational checklists, follow-up actions, or next-check suggestions unless the user explicitly requests them.
		Do not guess numbers, causes, or plant states that are not present in the reference data.
		Only use alarm level, alarm type, alarm message, facility name, and facility status when the question is about alarms or abnormalities.
		When web search results are available, use them only for external context and clearly separate them from internal SCADA data.
		For trend questions, use the dailyEnergyTrend and trendInsights data with dates and units.
		For line usage questions, use facilityLineUsageSummary and name the top facilities when available.
		When mentioning electricity, use kWh. When mentioning peak power, use kW.
		For electricity bill or tariff questions, use electricityBillComparison from the reference data only.
		Describe electricity bill values as estimates, not as official invoices.
		For electricity bill answers, include the plant period, tariff name, KRW amount, billing demand in kW, and excluded items when available.
		When mentioning ESG, include both the score and grade if they are available.
		""";

	private final OpenAiProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public OpenAiChatbotClient(OpenAiProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.restClient = restClientBuilder
			.baseUrl(properties.normalizedBaseUrl())
			.requestFactory(requestFactory(properties.timeoutSeconds()))
			.build();
	}

	public Optional<ChatbotAiResponse> generateResponse(
		String question,
		String referencedData,
		List<ChatbotMessage> recentMessages
	) {
		if (!properties.hasApiKey()) {
			return Optional.empty();
		}

		try {
			JsonNode response = restClient.post()
				.uri("/responses")
				.contentType(MediaType.APPLICATION_JSON)
				.headers(headers -> headers.setBearerAuth(properties.getApiKey()))
				.body(buildRequestBody(question, referencedData, recentMessages))
				.retrieve()
				.body(JsonNode.class);

			return extractResponse(response);
		}
		catch (RestClientResponseException exception) {
			log.warn(
				"OpenAI chatbot response generation failed. status={}, body={}",
				exception.getStatusCode(),
				truncate(exception.getResponseBodyAsString(), 500)
			);
			return Optional.empty();
		}
		catch (RestClientException exception) {
			log.warn("OpenAI chatbot response generation failed. reason={}, message={}", exception.getClass().getSimpleName(), exception.getMessage());
			return Optional.empty();
		}
	}

	public Optional<String> generateAnswer(
		String question,
		String referencedData,
		List<ChatbotMessage> recentMessages
	) {
		return generateResponse(question, referencedData, recentMessages).map(ChatbotAiResponse::answer);
	}

	Optional<String> extractOutputText(JsonNode response) {
		return extractResponse(response).map(ChatbotAiResponse::answer);
	}

	Optional<ChatbotAiResponse> extractResponse(JsonNode response) {
		if (response == null || response.hasNonNull("error")) {
			return Optional.empty();
		}
		if (!"completed".equals(response.path("status").asText())) {
			return Optional.empty();
		}

		List<String> outputTexts = new ArrayList<>();
		List<Map<String, String>> sources = new ArrayList<>();
		String imageDataUrl = null;
		for (JsonNode outputItem : response.path("output")) {
			if ("image_generation_call".equals(outputItem.path("type").asText())) {
				String imageBase64 = outputItem.path("result").asText("");
				if (!imageBase64.isBlank()) {
					imageDataUrl = "data:image/png;base64," + imageBase64;
				}
			}
			for (JsonNode contentItem : outputItem.path("content")) {
				if ("output_text".equals(contentItem.path("type").asText())) {
					String text = contentItem.path("text").asText("");
					if (!text.isBlank()) {
						outputTexts.add(text.trim());
					}
					extractSources(contentItem.path("annotations"), sources);
				}
			}
		}

		String answer = String.join("\n", outputTexts).trim();
		if (answer.isBlank() && imageDataUrl == null) {
			return Optional.empty();
		}
		if (answer.isBlank()) {
			answer = "요청한 이미지를 생성했습니다.";
		}
		return Optional.of(new ChatbotAiResponse(answer, null, imageDataUrl, serializeSources(sources)));
	}

	private Map<String, Object> buildRequestBody(
		String question,
		String referencedData,
		List<ChatbotMessage> recentMessages
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.getModel());
		body.put("instructions", INSTRUCTIONS);
		body.put("input", buildInput(question, referencedData, recentMessages));
		body.put("max_output_tokens", Math.max(properties.maxOutputTokens(), 900));
		List<Map<String, Object>> tools = buildTools(question);
		if (!tools.isEmpty()) {
			body.put("tools", tools);
			body.put("tool_choice", "auto");
		}
		return body;
	}

	private String buildInput(String question, String referencedData, List<ChatbotMessage> recentMessages) {
		return """
			User question:
			%s

			Reference data as JSON:
			%s

			Recent conversation:
			%s
			""".formatted(question, referencedData, formatRecentMessages(recentMessages));
	}

	private String formatRecentMessages(List<ChatbotMessage> recentMessages) {
		if (recentMessages == null || recentMessages.isEmpty()) {
			return "None";
		}

		List<ChatbotMessage> chronologicalMessages = new ArrayList<>(recentMessages);
		Collections.reverse(chronologicalMessages);
		List<String> lines = new ArrayList<>();
		for (ChatbotMessage message : chronologicalMessages) {
			lines.add("User: " + truncate(message.getQuestion(), 300));
			lines.add("Assistant: " + truncate(message.getAnswer(), 600));
		}
		return String.join("\n", lines);
	}

	private String truncate(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
	}

	private List<Map<String, Object>> buildTools(String question) {
		List<Map<String, Object>> tools = new ArrayList<>();
		if (properties.isWebSearchEnabled() && shouldUseWebSearch(question)) {
			Map<String, Object> webSearch = new LinkedHashMap<>();
			webSearch.put("type", "web_search_preview");
			webSearch.put("search_context_size", "medium");
			webSearch.put("user_location", Map.of(
				"type", "approximate",
				"country", "KR",
				"timezone", "Asia/Seoul"
			));
			tools.add(webSearch);
		}
		if (properties.isFileSearchEnabled() && properties.hasVectorStoreId()) {
			tools.add(Map.of(
				"type", "file_search",
				"vector_store_ids", List.of(properties.getVectorStoreId())
			));
		}
		if (properties.isImageGenerationEnabled() && shouldGenerateImage(question)) {
			tools.add(Map.of("type", "image_generation"));
		}
		return tools;
	}

	private boolean shouldUseWebSearch(String question) {
		String normalizedQuestion = normalize(question);
		return containsAny(
			normalizedQuestion,
			"외부", "검색", "최신", "뉴스", "인터넷", "웹", "시장", "전력거래", "탄소배출권", "정책", "규제",
			"날씨", "기상", "전기요금", "요금", "원자재", "비교 자료"
		);
	}

	private boolean shouldGenerateImage(String question) {
		String normalizedQuestion = normalize(question);
		return containsAny(normalizedQuestion, "이미지", "그림", "도식", "인포그래픽", "시각 자료", "포스터", "도출");
	}

	private boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) {
			if (value.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase();
	}

	private void extractSources(JsonNode annotations, List<Map<String, String>> sources) {
		if (annotations == null || !annotations.isArray()) {
			return;
		}
		for (JsonNode annotation : annotations) {
			String type = annotation.path("type").asText("");
			String url = annotation.path("url").asText("");
			if (!"url_citation".equals(type) || url.isBlank()) {
				continue;
			}
			Map<String, String> source = new LinkedHashMap<>();
			source.put("title", annotation.path("title").asText(url));
			source.put("url", url);
			sources.add(source);
		}
	}

	private String serializeSources(List<Map<String, String>> sources) {
		if (sources.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(sources);
		}
		catch (Exception exception) {
			return null;
		}
	}

	private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Duration timeout = Duration.ofSeconds(timeoutSeconds);
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}
}
