package com.smartfactory.scada.plant.dto;

import java.math.BigDecimal;

import com.smartfactory.scada.plant.domain.CompanyType;
import com.smartfactory.scada.plant.domain.Plant;

public record PlantResponse(
	Long id,
	String name,
	CompanyType companyType,
	String address,
	BigDecimal latitude,
	BigDecimal longitude
) {

	public static PlantResponse from(Plant plant) {
		return new PlantResponse(
			plant.getId(),
			plant.getName(),
			plant.getCompanyType(),
			plant.getAddress(),
			plant.getLatitude(),
			plant.getLongitude()
		);
	}
}
