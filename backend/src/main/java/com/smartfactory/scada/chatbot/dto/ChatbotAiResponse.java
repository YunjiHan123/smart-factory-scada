package com.smartfactory.scada.chatbot.dto;

public record ChatbotAiResponse(
	String answer,
	String chartSpec,
	String imageDataUrl,
	String externalSources
) {

	public static ChatbotAiResponse answerOnly(String answer) {
		return new ChatbotAiResponse(answer, null, null, null);
	}

	public ChatbotAiResponse withChartSpec(String chartSpec) {
		return new ChatbotAiResponse(answer, chartSpec, imageDataUrl, externalSources);
	}
}
