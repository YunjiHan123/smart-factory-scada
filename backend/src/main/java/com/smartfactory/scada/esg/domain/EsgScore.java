package com.smartfactory.scada.esg.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EsgScore {

	private Long id;
	private Long plantId;
	private String plantName;
	private LocalDate targetMonth;
	private BigDecimal electricityScore;
	private BigDecimal gasScore;
	private BigDecimal waterScore;
	private BigDecimal solarScore;
	private BigDecimal peakScore;
	private BigDecimal carbonScore;
	private BigDecimal totalScore;
	private EsgGrade grade;
}
