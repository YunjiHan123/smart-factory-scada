package com.smartfactory.scada.energy.domain;

import java.time.LocalDate;

public enum ElectricitySeason {
	SUMMER("여름철"),
	SPRING_AUTUMN("봄/가을철"),
	WINTER("겨울철");

	private final String displayName;

	ElectricitySeason(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public static ElectricitySeason from(LocalDate date) {
		int month = date.getMonthValue();
		if (month >= 6 && month <= 8) {
			return SUMMER;
		}
		if (month >= 3 && month <= 5 || month >= 9 && month <= 10) {
			return SPRING_AUTUMN;
		}
		return WINTER;
	}
}
