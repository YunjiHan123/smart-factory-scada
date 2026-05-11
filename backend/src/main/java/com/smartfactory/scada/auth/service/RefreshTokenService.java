package com.smartfactory.scada.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

	private static final String KEY_PREFIX = "auth:refresh:";

	private final StringRedisTemplate redisTemplate;
	private final Duration refreshTokenTtl;

	public RefreshTokenService(
		StringRedisTemplate redisTemplate,
		@Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
	) {
		this.redisTemplate = redisTemplate;
		this.refreshTokenTtl = Duration.ofMillis(refreshTokenExpirationMs);
	}

	public void save(Long userId, String refreshToken) {
		redisTemplate.opsForValue().set(key(userId), hash(refreshToken), refreshTokenTtl);
	}

	public boolean exists(Long userId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
	}

	public boolean matches(Long userId, String refreshToken) {
		String savedRefreshTokenHash = redisTemplate.opsForValue().get(key(userId));
		return hash(refreshToken).equals(savedRefreshTokenHash);
	}

	public void delete(Long userId) {
		redisTemplate.delete(key(userId));
	}

	String key(Long userId) {
		return KEY_PREFIX + userId;
	}

	String hash(String refreshToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashedBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
		}
	}
}
