package com.smartfactory.scada.energy.domain;

import java.math.BigDecimal;

public enum EnergyType {
	ELECTRICITY("kWh") {
		@Override
		public BigDecimal usage(EnergySummary summary) {
			return summary.getElectricityKwh();
		}
	},
	GAS("m3") {
		@Override
		public BigDecimal usage(EnergySummary summary) {
			return summary.getGasM3();
		}
	},
	WATER("ton") {
		@Override
		public BigDecimal usage(EnergySummary summary) {
			return summary.getWaterTon();
		}
	},
	SOLAR("kWh") {
		@Override
		public BigDecimal usage(EnergySummary summary) {
			return summary.getSolarKwh();
		}
	};

	private final String unit;

	EnergyType(String unit) {
		this.unit = unit;
	}

	public String unit() {
		return unit;
	}

	public abstract BigDecimal usage(EnergySummary summary);
}
