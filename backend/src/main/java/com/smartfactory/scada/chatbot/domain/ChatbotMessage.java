package com.smartfactory.scada.chatbot.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotMessage {

	private Long id;
	private Long userId;
	private Long plantId;
	private String plantName;
	private String question;
	private String answer;
	private String referencedData;
	private LocalDateTime createdAt;
	private String key;
}
