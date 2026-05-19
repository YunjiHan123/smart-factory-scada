package com.smartfactory.scada.energy.domain;

public enum ElectricityLoadPeriod {
	OFF_PEAK("경부하"),
	MID_PEAK("중간부하"),
	ON_PEAK("최대부하");

	private final String displayName;

	ElectricityLoadPeriod(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
