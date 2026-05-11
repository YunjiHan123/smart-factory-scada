package com.smartfactory.scada.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessTokenExpirationMs;
	private final long refreshTokenExpirationMs;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
		@Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMs = accessTokenExpirationMs;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
	}

	public String createAccessToken(Long userId) {
		return createToken(userId, TokenType.ACCESS, accessTokenExpirationMs);
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, TokenType.REFRESH, refreshTokenExpirationMs);
	}

	public TokenStatus validateToken(String token, TokenType expectedTokenType) {
		try {
			TokenType tokenType = getTokenType(parseClaims(token));
			if (tokenType != expectedTokenType) {
				return TokenStatus.WRONG_TYPE;
			}

			return TokenStatus.VALID;
		} catch (ExpiredJwtException exception) {
			return TokenStatus.EXPIRED;
		} catch (JwtException | IllegalArgumentException exception) {
			return TokenStatus.INVALID;
		}
	}

	public Long getUserId(String token) {
		return Long.valueOf(parseClaims(token).getSubject());
	}

	public Long getUserIdAllowExpired(String token, TokenType expectedTokenType) {
		Claims claims = parseClaimsAllowExpired(token);
		TokenType tokenType = getTokenType(claims);
		if (tokenType != expectedTokenType) {
			throw new IllegalArgumentException("Unexpected token type.");
		}

		return Long.valueOf(claims.getSubject());
	}

	private String createToken(Long userId, TokenType tokenType, long expirationMs) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusMillis(expirationMs);

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim("tokenType", tokenType.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(secretKey)
			.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private Claims parseClaimsAllowExpired(String token) {
		try {
			return parseClaims(token);
		} catch (ExpiredJwtException exception) {
			return exception.getClaims();
		}
	}

	private TokenType getTokenType(Claims claims) {
		String tokenType = claims.get("tokenType", String.class);
		return TokenType.valueOf(tokenType);
	}
}
