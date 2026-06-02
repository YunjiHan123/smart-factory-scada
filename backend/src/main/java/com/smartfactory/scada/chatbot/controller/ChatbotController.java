package com.smartfactory.scada.chatbot.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageRequest;
import com.smartfactory.scada.chatbot.dto.ChatbotMessageResponse;
import com.smartfactory.scada.chatbot.service.ChatbotService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chatbot/messages")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChatbotController {

	private final ChatbotService chatbotService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChatbotMessageResponse ask(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody ChatbotMessageRequest request
	) {
		return chatbotService.ask(authenticatedUser, request);
	}

	@GetMapping
	public List<ChatbotMessageResponse> getRecent(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(required = false) Long plantId,
		@RequestParam(required = false) Integer limit
	) {
		return chatbotService.getRecent(authenticatedUser, plantId, limit);
	}

	@DeleteMapping("/{messageId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMessage(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long messageId
	) {
		chatbotService.deleteMessage(authenticatedUser, messageId);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePlantMessages(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam Long plantId
	) {
		chatbotService.deleteMessagesByPlant(authenticatedUser, plantId);
	}
}
