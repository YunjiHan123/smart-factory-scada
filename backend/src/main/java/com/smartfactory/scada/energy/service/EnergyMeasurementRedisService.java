package com.smartfactory.scada.energy.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.energy.dto.EnergyMeasurementMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyMeasurementRedisService {

	private static final Duration LATEST_ENERGY_TTL = Duration.ofMinutes(5);
	private static final long REDIS_ERROR_LOG_INTERVAL_MS = Duration.ofSeconds(30).toMillis();

	private final AtomicLong lastRedisSaveErrorLogAt = new AtomicLong(0);
	private final AtomicLong lastRedisReadErrorLogAt = new AtomicLong(0);

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	public void saveLatest(EnergyMeasurementMessage message) {
		String redisKey = buildLatestKey(message.getPlantId(), message.getFacilityId());

		try {
			// Keep only the latest measurement for each facility as a JSON string.
			String redisValue = objectMapper.writeValueAsString(message);
			redisTemplate.opsForValue().set(redisKey, redisValue, LATEST_ENERGY_TTL);

			log.info(
				"Redis latest energy cached. key={}, plantId={}, facilityId={}, measuredAt={}",
				redisKey,
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt()
			);
		}
		catch (JsonProcessingException exception) {
			log.error(
				"Failed to serialize energy measurement before Redis save. plantId={}, facilityId={}, measuredAt={}",
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt(),
				exception
			);
		}
		catch (RedisConnectionFailureException exception) {
			logRedisConnectionWarning(
				lastRedisSaveErrorLogAt,
				"Redis is unavailable. Latest energy cache save will be skipped for now. key={}, plantId={}, facilityId={}, measuredAt={}, redisHostCheckHint=Check REDIS_HOST/REDIS_PORT and ensure Redis is running.",
				redisKey,
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt()
			);
		}
		catch (Exception exception) {
			log.error(
				"Failed to save latest energy measurement to Redis. key={}, plantId={}, facilityId={}, measuredAt={}",
				redisKey,
				message.getPlantId(),
				message.getFacilityId(),
				message.getMeasuredAt(),
				exception
			);
		}
	}

	public Optional<EnergyMeasurementMessage> findLatest(Long plantId, Long facilityId) {
		String redisKey = buildLatestKey(plantId, facilityId);

		try {
			String redisValue = redisTemplate.opsForValue().get(redisKey);
			if (redisValue == null) {
				return Optional.empty();
			}

			return Optional.of(objectMapper.readValue(redisValue, EnergyMeasurementMessage.class));
		}
		catch (RedisConnectionFailureException exception) {
			logRedisConnectionWarning(
				lastRedisReadErrorLogAt,
				"Redis is unavailable. Latest energy cache read will return empty for now. key={}, plantId={}, facilityId={}, redisHostCheckHint=Check REDIS_HOST/REDIS_PORT and ensure Redis is running.",
				redisKey,
				plantId,
				facilityId
			);
			return Optional.empty();
		}
		catch (Exception exception) {
			log.error(
				"Failed to read latest energy measurement from Redis. key={}, plantId={}, facilityId={}",
				redisKey,
				plantId,
				facilityId,
				exception
			);
			return Optional.empty();
		}
	}

	private String buildLatestKey(Long plantId, Long facilityId) {
		return "latest:energy:plant:" + plantId + ":facility:" + facilityId;
	}

	private void logRedisConnectionWarning(AtomicLong lastLoggedAt, String message, Object... arguments) {
		long now = System.currentTimeMillis();
		long previous = lastLoggedAt.get();
		if (now - previous < REDIS_ERROR_LOG_INTERVAL_MS) {
			return;
		}

		if (lastLoggedAt.compareAndSet(previous, now)) {
			log.warn(message, arguments);
		}
	}
}
