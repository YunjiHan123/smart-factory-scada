package com.smartfactory.scada.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

	private String apiKey = "";
	private String model = "gpt-5.4-mini";
	private String baseUrl = "https://api.openai.com/v1";
	private int timeoutSeconds = 10;
	private int maxOutputTokens = 600;
	private boolean webSearchEnabled = true;
	private boolean imageGenerationEnabled = true;
	private boolean fileSearchEnabled = true;
	private String vectorStoreId = "";

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	public String normalizedBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "https://api.openai.com/v1";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public int timeoutSeconds() {
		return timeoutSeconds <= 0 ? 10 : timeoutSeconds;
	}

	public int maxOutputTokens() {
		return maxOutputTokens <= 0 ? 600 : maxOutputTokens;
	}

	public boolean hasVectorStoreId() {
		return vectorStoreId != null && !vectorStoreId.isBlank();
	}
}
