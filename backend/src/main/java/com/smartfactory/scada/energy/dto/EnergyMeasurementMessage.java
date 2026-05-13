package com.smartfactory.scada.energy.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EnergyMeasurementMessage {

	private Long plantId;
	private Long facilityId;
	private Instant measuredAt;
	private Double electricityKwh;
	private Double gasM3;
	private Double waterTon;
	private Double solarKwh;
	private Double peakKw;
}
