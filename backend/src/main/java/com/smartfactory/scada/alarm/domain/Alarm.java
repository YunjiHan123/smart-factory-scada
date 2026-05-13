package com.smartfactory.scada.alarm.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Alarm {

	private Long id;
	private Long plantId;
	private Long facilityId;
	private String plantName;
	private String facilityName;
	private AlarmType alarmType;
	private AlarmLevel alarmLevel;
	private String message;
	private BigDecimal value;
	private BigDecimal thresholdValue;
	private LocalDateTime occurredAt;
	private LocalDateTime resolvedAt;
	private AlarmStatus status;
}
