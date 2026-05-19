package com.smartfactory.scada.energy.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilityUsagePlantComparison {

	private Long plantId;
	private String plantName;
	private BigDecimal gasUsageM3;
	private BigDecimal waterUsageTon;
	private BigDecimal gasShareRate;
	private BigDecimal waterShareRate;
	private Integer gasRank;
	private Integer waterRank;
}
