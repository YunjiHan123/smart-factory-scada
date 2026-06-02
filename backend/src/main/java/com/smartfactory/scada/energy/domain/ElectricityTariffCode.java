package com.smartfactory.scada.energy.domain;

public enum ElectricityTariffCode {
	HIGH_VOLTAGE_A_OPTION_I("고압A 선택I"),
	HIGH_VOLTAGE_A_OPTION_II("고압A 선택II"),
	HIGH_VOLTAGE_A_OPTION_III("고압A 선택III"),
	HIGH_VOLTAGE_B_OPTION_I("고압B 선택I"),
	HIGH_VOLTAGE_B_OPTION_II("고압B 선택II"),
	HIGH_VOLTAGE_B_OPTION_III("고압B 선택III"),
	HIGH_VOLTAGE_C_OPTION_I("고압C 선택I"),
	HIGH_VOLTAGE_C_OPTION_II("고압C 선택II"),
	HIGH_VOLTAGE_C_OPTION_III("고압C 선택III");

	private final String displayName;

	ElectricityTariffCode(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
