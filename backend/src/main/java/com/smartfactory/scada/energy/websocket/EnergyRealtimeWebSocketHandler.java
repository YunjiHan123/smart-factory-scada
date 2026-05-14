package com.smartfactory.scada.energy.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnergyRealtimeWebSocketHandler extends TextWebSocketHandler {

	private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
	private final ObjectMapper objectMapper;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.add(session);
		log.info("Energy realtime WebSocket connected. sessionId={}, activeSessions={}", session.getId(), sessions.size());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session);
		log.info(
			"Energy realtime WebSocket disconnected. sessionId={}, status={}, activeSessions={}",
			session.getId(),
			status,
			sessions.size()
		);
	}

	public void broadcast(EnergyMeasurementMessage message) {
		if (sessions.isEmpty()) {
			return;
		}

		String payload;
		try {
			payload = objectMapper.writeValueAsString(message);
		}
		catch (JsonProcessingException exception) {
			log.error(
				"Failed to serialize realtime energy WebSocket message. plantId={}, facilityId={}, measuredAt={}",
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt(),
				exception
			);
			return;
		}

		TextMessage textMessage = new TextMessage(payload);
		for (WebSocketSession session : sessions) {
			send(session, textMessage);
		}
	}

	private void send(WebSocketSession session, TextMessage message) {
		if (!session.isOpen()) {
			sessions.remove(session);
			return;
		}

		try {
			synchronized (session) {
				session.sendMessage(message);
			}
		}
		catch (IOException exception) {
			sessions.remove(session);
			log.warn("Failed to send realtime energy WebSocket message. sessionId={}", session.getId(), exception);
		}
	}
}
