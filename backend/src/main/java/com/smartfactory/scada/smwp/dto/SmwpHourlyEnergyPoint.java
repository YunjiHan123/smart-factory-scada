package com.smartfactory.scada.smwp.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmwpHourlyEnergyPoint {

	private Integer hour;
	private BigDecimal electricityKwh;
	private BigDecimal gasM3;
	private BigDecimal waterTon;
	private BigDecimal solarKwh;
}
