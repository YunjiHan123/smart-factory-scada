package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartfactory.scada.energy.domain.EnergyMeasurement;

public record EnergyMeasurementResponse(
	Long id,
	Long plantId,
	Long facilityId,
	LocalDateTime measuredAt,
	BigDecimal electricityKwh,
	BigDecimal gasM3,
	BigDecimal waterTon,
	BigDecimal solarKwh,
	BigDecimal peakKw
) {

	public static EnergyMeasurementResponse from(EnergyMeasurement measurement) {
		return new EnergyMeasurementResponse(
			measurement.getId(),
			measurement.getPlantId(),
			measurement.getFacilityId(),
			measurement.getMeasuredAt(),
			measurement.getElectricityKwh(),
			measurement.getGasM3(),
			measurement.getWaterTon(),
			measurement.getSolarKwh(),
			measurement.getPeakKw()
		);
	}
}
