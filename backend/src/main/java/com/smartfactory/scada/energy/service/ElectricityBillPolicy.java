package com.smartfactory.scada.energy.service;

import java.util.List;

final class ElectricityBillPolicy {

	static final String DEMAND_UNIT = "kW";
	static final String USAGE_UNIT = "kWh";
	static final String CURRENCY_UNIT = "KRW";
	static final List<String> ASSUMPTIONS = List.of(
		"피크 화면 의사결정용 추정 전기요금입니다.",
		"기본요금은 선택 기간 내 15분 최대 피크전력을 기준으로 산정합니다.",
		"부가가치세, 전력산업기반기금, 기후환경요금, 연료비조정액, 복지/휴일/토요일 할인은 제외합니다.",
		"요금 단가는 사용자가 첨부한 한전 산업용전력(을) 요금표 v1 기준 데이터입니다."
	);

	private ElectricityBillPolicy() {
	}
}
