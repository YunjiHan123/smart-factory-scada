package com.smartfactory.scada.energy.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.energy.domain.EnergyMeasurement;
import com.smartfactory.scada.energy.domain.EnergySummary;
import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.dto.EnergyFacilityLineUsageResponse;
import com.smartfactory.scada.energy.dto.PeakPowerFacilityRanking;
import com.smartfactory.scada.energy.dto.PeakPowerHistory;
import com.smartfactory.scada.energy.dto.PeakPowerTrendPoint;
import com.smartfactory.scada.energy.dto.UtilityHourlyUsage;
import com.smartfactory.scada.energy.dto.UtilityMeterStatus;
import com.smartfactory.scada.energy.dto.UtilityUsagePattern;
import com.smartfactory.scada.facility.domain.FacilityType;

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

	int insertHourlyFacilitySummaries(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	int insertHourlyPlantSummaries(
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

	Optional<LocalDate> findFacilityLineSummaryDate(
		@Param("plantId") Long plantId,
		@Param("facilityType") FacilityType facilityType,
		@Param("targetDate") LocalDate targetDate
	);

	Optional<LocalDate> findLatestFacilityLineSummaryDate(
		@Param("plantId") Long plantId,
		@Param("facilityType") FacilityType facilityType
	);

	Optional<LocalDate> findLatestFacilityLineMeasurementDate(
		@Param("plantId") Long plantId,
		@Param("facilityType") FacilityType facilityType
	);

	List<EnergyFacilityLineUsageResponse> findFacilityLineUsages(
		@Param("plantId") Long plantId,
		@Param("facilityType") FacilityType facilityType,
		@Param("energyType") String energyType,
		@Param("usageDate") LocalDate usageDate,
		@Param("yesterdayDate") LocalDate yesterdayDate,
		@Param("monthStart") LocalDate monthStart,
		@Param("nextMonthStart") LocalDate nextMonthStart,
		@Param("usageFrom") LocalDateTime usageFrom,
		@Param("usageTo") LocalDateTime usageTo
	);

	Optional<EnergyMeasurement> findLatestMeasurement(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId
	);

	Optional<EnergyMeasurement> findPreviousMeasurement(
		@Param("plantId") Long plantId,
		@Param("facilityId") Long facilityId,
		@Param("measuredAt") LocalDateTime measuredAt
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

	List<UtilityHourlyUsage> findUtilityHourlyUsage(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<UtilityHourlyUsage> findUtilityUsageTotal(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	Optional<EnergyMeasurement> findLatestPlantUtilityMeasurement(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityMeterStatus> findUtilityMeterStatuses(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<UtilityUsagePattern> findUtilityDailyUsagePatterns(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);
}
