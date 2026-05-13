package com.smartfactory.scada.facility.domain;

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
public class Facility {

	private Long id;
	private Long plantId;
	private String name;
	private FacilityType facilityType;
	private FacilityStatus status;
}
