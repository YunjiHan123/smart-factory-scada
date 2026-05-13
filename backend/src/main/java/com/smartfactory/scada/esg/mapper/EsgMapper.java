package com.smartfactory.scada.esg.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.esg.domain.EsgScore;

@Mapper
public interface EsgMapper {

	List<EsgScore> findScores(@Param("targetMonth") LocalDate targetMonth);

	Optional<EsgScore> findLatestByPlantId(@Param("plantId") Long plantId);
}
