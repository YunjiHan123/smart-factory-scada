package com.smartfactory.scada.simulation.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.simulation.domain.SimulationResult;

@Mapper
public interface SimulationMapper {

	void insert(SimulationResult simulationResult);

	Optional<SimulationResult> findById(@Param("id") Long id);

	List<SimulationResult> findRecent(
		@Param("userId") Long userId,
		@Param("plantId") Long plantId,
		@Param("limit") int limit
	);
}
