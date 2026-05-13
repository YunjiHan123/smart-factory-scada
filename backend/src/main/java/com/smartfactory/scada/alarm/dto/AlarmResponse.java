package com.smartfactory.scada.alarm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartfactory.scada.alarm.domain.Alarm;
import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.domain.AlarmType;

public record AlarmResponse(
	Long id,
	Long plantId,
	Long facilityId,
	String plantName,
	String facilityName,
	AlarmType alarmType,
	AlarmLevel alarmLevel,
	String message,
	BigDecimal value,
	BigDecimal thresholdValue,
	LocalDateTime occurredAt,
	LocalDateTime resolvedAt,
	AlarmStatus status
) {

	public static AlarmResponse from(Alarm alarm) {
		return new AlarmResponse(
			alarm.getId(),
			alarm.getPlantId(),
			alarm.getFacilityId(),
			alarm.getPlantName(),
			alarm.getFacilityName(),
			alarm.getAlarmType(),
			alarm.getAlarmLevel(),
			alarm.getMessage(),
			alarm.getValue(),
			alarm.getThresholdValue(),
			alarm.getOccurredAt(),
			alarm.getResolvedAt(),
			alarm.getStatus()
		);
	}
}
