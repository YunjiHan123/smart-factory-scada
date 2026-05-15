package com.smartfactory.scada.energy.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.dto.PeakPowerFacilityRanking;
import com.smartfactory.scada.energy.dto.PeakPowerHistory;
import com.smartfactory.scada.energy.dto.PeakPowerTrendPoint;

@Mapper
public interface EnergyMapper {

	void insertMeasurement(EnergyMeasurement measurement);

	int deleteSummaries(
		@Param("summaryType") SummaryType summaryType,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	int insertDailyFacilitySummaries(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	int insertDailyPlantSummaries(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	int insertMonthlyFacilitySummaries(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	int insertMonthlyPlantSummaries(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

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

	List<EnergySummary> findMeasurementDailySums(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<EnergyMeasurement> findLatestMeasurement(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId
	);

	Optional<EnergySummary> findLatestPlantSummary(@Param("plantId") Long plantId);

	List<PeakPowerTrendPoint> findPeakPowerTrend(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<PeakPowerFacilityRanking> findPeakPowerFacilityRanking(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("limit") int limit
	);

	List<PeakPowerHistory> findPeakPowerHistory(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("thresholdKw") java.math.BigDecimal thresholdKw,
		@Param("limit") int limit
	);

	Optional<EnergyMeasurement> findLatestPlantMeasurement(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);
}
