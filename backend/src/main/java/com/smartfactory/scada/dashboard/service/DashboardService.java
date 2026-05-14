package com.smartfactory.scada.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.alarm.domain.AlarmStatus;
import com.smartfactory.scada.alarm.dto.AlarmResponse;
import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.dashboard.dto.DashboardOverviewResponse;
import com.smartfactory.scada.energy.dto.EnergySummaryResponse;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.esg.dto.EsgScoreResponse;
import com.smartfactory.scada.esg.mapper.EsgMapper;
import com.smartfactory.scada.facility.dto.FacilityResponse;
import com.smartfactory.scada.facility.mapper.FacilityMapper;
import com.smartfactory.scada.plant.dto.PlantResponse;
import com.smartfactory.scada.plant.mapper.PlantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final PlantMapper plantMapper;
	private final FacilityMapper facilityMapper;
	private final EnergyMapper energyMapper;
	private final EsgMapper esgMapper;
	private final AlarmMapper alarmMapper;

	@Transactional(readOnly = true)
	public DashboardOverviewResponse getOverview(Long plantId) {
		List<PlantResponse> plants = plantMapper.findAll()
			.stream()
			.map(PlantResponse::from)
			.toList();
		Long selectedPlantId = resolvePlantId(plantId, plants);

		List<FacilityResponse> facilities = selectedPlantId == null
			? List.of()
			: facilityMapper.findByPlantId(selectedPlantId)
				.stream()
				.map(FacilityResponse::from)
				.toList();

		EnergySummaryResponse latestEnergySummary = selectedPlantId == null
			? null
			: energyMapper.findLatestPlantSummary(selectedPlantId)
				.map(EnergySummaryResponse::from)
				.orElse(null);
		EsgScoreResponse latestEsgScore = selectedPlantId == null
			? null
			: esgMapper.findLatestByPlantId(selectedPlantId)
				.map(EsgScoreResponse::from)
				.orElse(null);
		List<AlarmResponse> recentAlarms = alarmMapper.findAlarms(selectedPlantId, AlarmStatus.OCCURRED, null, 10)
			.stream()
			.map(AlarmResponse::from)
			.toList();

		return new DashboardOverviewResponse(
			plants,
			facilities,
			latestEnergySummary,
			latestEsgScore,
			recentAlarms,
			alarmMapper.countOccurred(selectedPlantId)
		);
	}

	private Long resolvePlantId(Long plantId, List<PlantResponse> plants) {
		if (plantId != null) {
			return plantId;
		}
		return plants.isEmpty() ? null : plants.get(0).id();
	}
}
