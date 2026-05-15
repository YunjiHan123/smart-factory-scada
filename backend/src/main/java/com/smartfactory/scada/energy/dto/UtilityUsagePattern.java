package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilityUsagePattern {

	private LocalDate usageDate;
	private BigDecimal gasUsageM3;
	private BigDecimal waterUsageTon;
	private BigDecimal gasUsageRate;
	private BigDecimal waterUsageRate;
}
