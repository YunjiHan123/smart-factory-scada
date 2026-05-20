package com.smartfactory.scada.smwp.dto;

public record SmwpAlarmResponse(
	String occurredAt,
	String message
) {
}
