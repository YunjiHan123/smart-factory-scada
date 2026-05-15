package com.smartfactory.scada.dashboard.dto;

import java.util.List;

import com.smartfactory.scada.alarm.dto.AlarmResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.esg.dto.EsgScoreResponse;
import com.smartfactory.scada.facility.dto.FacilityResponse;
import com.smartfactory.scada.plant.dto.PlantResponse;

public record DashboardOverviewResponse(
	List<PlantResponse> plants,
	List<FacilityResponse> facilities,
	EnergySummaryResponse latestEnergySummary,
	EsgScoreResponse latestEsgScore,
	List<AlarmResponse> recentAlarms,
	long occurredAlarmCount
) {
}
