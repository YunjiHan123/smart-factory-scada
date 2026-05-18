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

import com.fasterxml.jackson.databind.JsonNode;
import com.smartfactory.scada.chatbot.config.OpenAiProperties;
import com.smartfactory.scada.chatbot.domain.ChatbotMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenAiChatbotClient {

	private static final String INSTRUCTIONS = """
		You are the chatbot for an energy integrated control system used by Hyundai and Kia plants.
		Answer in Korean. Keep the answer concise and useful for plant operators.
		Do not guess numbers, causes, or plant states that are not present in the reference data.
		When mentioning electricity, use kWh. When mentioning peak power, use kW.
		When mentioning ESG, include both the score and grade if they are available.
		If useful, suggest one next check in a single sentence.
		""";

	private final OpenAiProperties properties;
	private final RestClient restClient;

	public OpenAiChatbotClient(OpenAiProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder
			.baseUrl(properties.normalizedBaseUrl())
			.requestFactory(requestFactory(properties.timeoutSeconds()))
			.build();
	}

	public Optional<String> generateAnswer(
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

			return extractOutputText(response);
		}
		catch (RestClientException exception) {
			log.warn("OpenAI chatbot response generation failed. reason={}", exception.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	Optional<String> extractOutputText(JsonNode response) {
		if (response == null || response.hasNonNull("error")) {
			return Optional.empty();
		}
		if (!"completed".equals(response.path("status").asText())) {
			return Optional.empty();
		}

		List<String> outputTexts = new ArrayList<>();
		for (JsonNode outputItem : response.path("output")) {
			for (JsonNode contentItem : outputItem.path("content")) {
				if ("output_text".equals(contentItem.path("type").asText())) {
					String text = contentItem.path("text").asText("");
					if (!text.isBlank()) {
						outputTexts.add(text.trim());
					}
				}
			}
		}

		String answer = String.join("\n", outputTexts).trim();
		return answer.isBlank() ? Optional.empty() : Optional.of(answer);
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
		body.put("max_output_tokens", properties.maxOutputTokens());
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

	private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Duration timeout = Duration.ofSeconds(timeoutSeconds);
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}
}
