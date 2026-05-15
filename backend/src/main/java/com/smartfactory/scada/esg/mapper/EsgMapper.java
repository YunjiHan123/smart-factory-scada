package com.smartfactory.scada.esg.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.esg.domain.EsgScore;
import com.smartfactory.scada.esg.domain.EsgEnergyAggregate;

@Mapper
public interface EsgMapper {

	List<EsgScore> findScores(@Param("targetMonth") LocalDate targetMonth);

	Optional<EsgScore> findLatestByPlantId(@Param("plantId") Long plantId);

	List<EsgEnergyAggregate> findEnvironmentAggregates(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	List<EsgEnergyAggregate> findEnvironmentAggregatesFromSummaries(
		@Param("plantId") Long plantId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);
}
