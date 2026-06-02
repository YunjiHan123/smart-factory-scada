package com.smartfactory.scada.facility.dto;

import com.smartfactory.scada.facility.domain.Facility;
import com.smartfactory.scada.facility.domain.FacilityStatus;
import com.smartfactory.scada.facility.domain.FacilityType;

public record FacilityResponse(
	Long id,
	Long plantId,
	String name,
	FacilityType facilityType,
	FacilityStatus status
) {

	public static FacilityResponse from(Facility facility) {
		return new FacilityResponse(
			facility.getId(),
			facility.getPlantId(),
			facility.getName(),
			facility.getFacilityType(),
			facility.getStatus()
		);
	}
}
