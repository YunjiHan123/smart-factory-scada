package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilityMeterStatus {

	private Long facilityId;
	private String facilityName;
	private String meterName;
	private String meterType;
	private BigDecimal currentValue;
	private String unit;
	private LocalDateTime lastReceivedAt;
	private String communicationStatus;
}
