package com.smartfactory.scada.plant.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plant {

	private Long id;
	private String name;
	private CompanyType companyType;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
}
