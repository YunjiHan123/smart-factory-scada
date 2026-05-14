package com.smartfactory.scada.alarm.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.alarm.domain.Alarm;
import com.smartfactory.scada.alarm.domain.AlarmLevel;
import com.smartfactory.scada.alarm.domain.AlarmStatus;

@Mapper
public interface AlarmMapper {

	List<Alarm> findAlarms(
		@Param("plantId") Long plantId,
		@Param("status") AlarmStatus status,
		@Param("alarmLevel") AlarmLevel alarmLevel,
		@Param("limit") int limit
	);

	Optional<Alarm> findById(@Param("id") Long id);

	int resolve(@Param("id") Long id);

	long countOccurred(@Param("plantId") Long plantId);
}
