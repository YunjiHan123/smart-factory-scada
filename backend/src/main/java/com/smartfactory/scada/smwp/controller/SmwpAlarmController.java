package com.smartfactory.scada.smwp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.smwp.dto.SmwpAlarmResponse;
import com.smartfactory.scada.smwp.service.SmwpAlarmService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/smwp/alarms")
public class SmwpAlarmController {

	private final SmwpAlarmService smwpAlarmService;

	@GetMapping
	public List<SmwpAlarmResponse> getAlarms(
		@RequestParam String plantName,
		@RequestParam(required = false) Integer limit
	) {
		return smwpAlarmService.getAlarms(plantName, limit);
	}
}
