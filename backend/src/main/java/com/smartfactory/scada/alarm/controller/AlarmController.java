package com.smartfactory.scada.alarm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.dto.AlarmResponse;
import com.smartfactory.scada.alarm.service.AlarmService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alarms")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AlarmController {

	private final AlarmService alarmService;

	@GetMapping
	public List<AlarmResponse> getAlarms(
		@RequestParam(required = false) Long plantId,
		@RequestParam(required = false) AlarmStatus status,
		@RequestParam(required = false) AlarmLevel alarmLevel,
		@RequestParam(required = false) Integer limit
	) {
		return alarmService.getAlarms(plantId, status, alarmLevel, limit);
	}

	@PatchMapping("/{alarmId}/resolve")
	public AlarmResponse resolve(@PathVariable Long alarmId) {
		return alarmService.resolve(alarmId);
	}

	@DeleteMapping("/{alarmId}")
	public ResponseEntity<Void> deleteResolved(@PathVariable Long alarmId) {
		alarmService.deleteResolved(alarmId);
		return ResponseEntity.noContent().build();
	}
}
