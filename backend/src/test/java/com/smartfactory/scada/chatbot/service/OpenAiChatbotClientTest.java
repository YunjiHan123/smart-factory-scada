package com.smartfactory.scada.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.chatbot.config.OpenAiProperties;

class OpenAiChatbotClientTest {

	private OpenAiChatbotClient client;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		OpenAiProperties properties = new OpenAiProperties();
		objectMapper = new ObjectMapper();
		client = new OpenAiChatbotClient(properties, RestClient.builder(), objectMapper);
	}

	@Test
	void generateAnswerReturnsEmptyWhenApiKeyIsMissing() {
		assertThat(client.generateAnswer("summary", "{}", List.of())).isEmpty();
	}

	@Test
	void extractOutputTextReturnsJoinedOutputText() throws JsonProcessingException {
		var response = objectMapper.readTree("""
			{
			  "status": "completed",
			  "error": null,
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        { "type": "output_text", "text": "first answer" },
			        { "type": "output_text", "text": "second answer" }
			      ]
			    }
			  ]
			}
			""");

		assertThat(client.extractOutputText(response)).contains("first answer\nsecond answer");
	}

	@Test
	void extractOutputTextReturnsEmptyWhenStatusIsNotCompleted() throws JsonProcessingException {
		var response = objectMapper.readTree("""
			{
			  "status": "in_progress",
			  "error": null,
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        { "type": "output_text", "text": "not ready" }
			      ]
			    }
			  ]
			}
			""");

		assertThat(client.extractOutputText(response)).isEmpty();
	}

	@Test
	void extractOutputTextReturnsEmptyWhenResponseHasError() throws JsonProcessingException {
		var response = objectMapper.readTree("""
			{
			  "status": "failed",
			  "error": { "message": "bad request" },
			  "output": []
			}
			""");

		assertThat(client.extractOutputText(response)).isEmpty();
	}

	@Test
	void extractOutputTextReturnsEmptyWhenOutputTextIsMissing() throws JsonProcessingException {
		var response = objectMapper.readTree("""
			{
			  "status": "completed",
			  "error": null,
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        { "type": "refusal", "text": "cannot answer" }
			      ]
			    }
			  ]
			}
			""");

		assertThat(client.extractOutputText(response)).isEmpty();
	}
}
