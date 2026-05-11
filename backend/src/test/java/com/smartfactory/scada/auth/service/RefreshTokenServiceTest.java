package com.smartfactory.scada.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	void saveStoresHashedRefreshTokenWithTtl() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);

		refreshTokenService.save(1L, "refresh-token");

		ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
		then(valueOperations).should().set(eq("auth:refresh:1"), valueCaptor.capture(), ttlCaptor.capture());
		assertThat(valueCaptor.getValue()).isNotEqualTo("refresh-token");
		assertThat(valueCaptor.getValue()).hasSize(64);
		assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void existsReturnsTrueWhenKeyExists() {
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);
		given(redisTemplate.hasKey("auth:refresh:1")).willReturn(true);

		boolean exists = refreshTokenService.exists(1L);

		assertThat(exists).isTrue();
	}

	@Test
	void matchesReturnsTrueWhenSavedHashMatches() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);
		given(valueOperations.get("auth:refresh:1")).willReturn(refreshTokenService.hash("refresh-token"));

		boolean matches = refreshTokenService.matches(1L, "refresh-token");

		assertThat(matches).isTrue();
	}

	@Test
	void matchesReturnsFalseWhenSavedHashDoesNotExist() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);
		given(valueOperations.get("auth:refresh:1")).willReturn(null);

		boolean matches = refreshTokenService.matches(1L, "refresh-token");

		assertThat(matches).isFalse();
	}

	@Test
	void matchesReturnsFalseWhenSavedHashIsDifferent() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);
		given(valueOperations.get("auth:refresh:1")).willReturn(refreshTokenService.hash("another-refresh-token"));

		boolean matches = refreshTokenService.matches(1L, "refresh-token");

		assertThat(matches).isFalse();
	}

	@Test
	void deleteRemovesRefreshTokenKey() {
		RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate, 3600000L);

		refreshTokenService.delete(1L);

		then(redisTemplate).should().delete("auth:refresh:1");
	}
}
