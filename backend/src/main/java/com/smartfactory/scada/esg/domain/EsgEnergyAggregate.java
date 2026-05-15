package com.smartfactory.scada.esg.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EsgEnergyAggregate {

	private Long plantId;
	private String plantName;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private Integer facilityCount;
	private Integer measurementCount;
	private LocalDateTime latestMeasuredAt;
	private BigDecimal electricityKwh;
	private BigDecimal gasM3;
	private BigDecimal waterTon;
	private BigDecimal solarKwh;
	private BigDecimal currentPeakKw;
	private BigDecimal maxPeakKw;
	private BigDecimal intervalMaxPeakKw;
}
