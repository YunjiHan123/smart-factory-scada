package com.smartfactory.scada.chatbot.dto;

import java.time.LocalDateTime;

import com.smartfactory.scada.chatbot.domain.ChatbotMessage;

public record ChatbotMessageResponse(
	Long id,
	Long userId,
	Long plantId,
	String plantName,
	String question,
	String answer,
	String referencedData,
	String chartSpec,
	String imageDataUrl,
	String externalSources,
	LocalDateTime createdAt
) {

	public static ChatbotMessageResponse from(ChatbotMessage message) {
		return new ChatbotMessageResponse(
			message.getId(),
			message.getUserId(),
			message.getPlantId(),
			message.getPlantName(),
			message.getQuestion(),
			message.getAnswer(),
			message.getReferencedData(),
			message.getChartSpec(),
			message.getImageDataUrl(),
			message.getExternalSources(),
			message.getCreatedAt()
		);
	}
}
