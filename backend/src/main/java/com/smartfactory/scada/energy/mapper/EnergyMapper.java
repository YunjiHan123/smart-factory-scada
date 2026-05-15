package com.smartfactory.scada.energy.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.SummaryType;

@Mapper
public interface EnergyMapper {

	void insertMeasurement(EnergyMeasurement measurement);

	List<EnergyMeasurement> findMeasurements(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("limit") int limit
	);

	List<EnergySummary> findSummaries(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId,
		@Param("summaryType") SummaryType summaryType,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<EnergyMeasurement> findLatestMeasurement(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId
	);

	Optional<EnergySummary> findLatestPlantSummary(@Param("plantId") Long plantId);
}
