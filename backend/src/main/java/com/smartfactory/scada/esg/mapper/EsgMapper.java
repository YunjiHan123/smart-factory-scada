package com.smartfactory.scada.esg.mapper;

import java.time.LocalDate;
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
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to
	);
}
