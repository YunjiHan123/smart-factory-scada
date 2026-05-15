package com.smartfactory.scada.energy.mapper;

import java.math.BigDecimal;
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
import com.smartfactory.scada.energy.dto.UtilityHourlyUsage;
import com.smartfactory.scada.energy.dto.UtilityMeterStatus;
import com.smartfactory.scada.energy.dto.UtilityUsagePattern;

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

	Optional<EnergyMeasurement> findLatestStoredMeasurement(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId
	);

	void upsertLatestMeasurement(EnergyMeasurement measurement);

	void upsertIntervalSummary(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId,
		@Param("bucketType") String bucketType,
		@Param("bucketAt") LocalDateTime bucketAt,
		@Param("electricityUsageKwh") BigDecimal electricityUsageKwh,
		@Param("gasUsageM3") BigDecimal gasUsageM3,
		@Param("waterUsageTon") BigDecimal waterUsageTon,
		@Param("solarUsageKwh") BigDecimal solarUsageKwh,
		@Param("peakKw") BigDecimal peakKw,
		@Param("measuredAt") LocalDateTime measuredAt
	);

	Optional<EnergySummary> findLatestPlantSummary(@Param("plantId") Long plantId);

	List<PeakPowerTrendPoint> findPeakPowerTrend(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<PeakPowerTrendPoint> findIntervalPeakPowerTrend(
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

	List<PeakPowerFacilityRanking> findIntervalPeakPowerFacilityRanking(
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

	Optional<EnergyMeasurement> findLatestPlantMeasurementFromLatest(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityHourlyUsage> findUtilityHourlyUsage(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityHourlyUsage> findIntervalUtilityHourlyUsage(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<EnergyMeasurement> findLatestPlantUtilityMeasurement(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<EnergyMeasurement> findLatestPlantUtilityMeasurementFromLatest(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityMeterStatus> findUtilityMeterStatuses(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityMeterStatus> findUtilityMeterStatusesFromLatest(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityUsagePattern> findUtilityDailyUsagePatterns(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityUsagePattern> findIntervalUtilityDailyUsagePatterns(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);
}
