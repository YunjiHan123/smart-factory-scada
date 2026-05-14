package com.smartfactory.scada.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotMessageRequest(
	Long plantId,
	@NotBlank String question
) {
}
