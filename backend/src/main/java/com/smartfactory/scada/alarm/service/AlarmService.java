package com.smartfactory.scada.alarm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.dto.AlarmResponse;
import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlarmService {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 200;

	private final AlarmMapper alarmMapper;

	@Transactional(readOnly = true)
	public List<AlarmResponse> getAlarms(Long plantId, AlarmStatus status, AlarmLevel alarmLevel, Integer limit) {
		return alarmMapper.findAlarms(plantId, status, alarmLevel, normalizeLimit(limit))
			.stream()
			.map(AlarmResponse::from)
			.toList();
	}

	@Transactional
	public AlarmResponse resolve(Long alarmId) {
		alarmMapper.resolve(alarmId);
		return alarmMapper.findById(alarmId)
			.map(AlarmResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.VALIDATION_ERROR));
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
