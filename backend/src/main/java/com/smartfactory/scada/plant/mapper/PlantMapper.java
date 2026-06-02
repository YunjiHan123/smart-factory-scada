package com.smartfactory.scada.plant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.plant.domain.Plant;

@Mapper
public interface PlantMapper {

	List<Plant> findAll();

	Optional<Plant> findById(@Param("id") Long id);
}
