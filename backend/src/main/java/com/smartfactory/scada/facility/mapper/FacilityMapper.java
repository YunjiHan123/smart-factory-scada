package com.smartfactory.scada.facility.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.facility.domain.Facility;

@Mapper
public interface FacilityMapper {

	List<Facility> findByPlantId(@Param("plantId") Long plantId);

	Optional<Facility> findById(@Param("id") Long id);
}
