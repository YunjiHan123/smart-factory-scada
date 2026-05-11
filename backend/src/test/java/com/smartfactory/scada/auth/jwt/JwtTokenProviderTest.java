package com.smartfactory.scada.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
		SECRET,
		300000L,
		3600000L
	);

	@Test
	void createAccessTokenReturnsToken() {
		String token = jwtTokenProvider.createAccessToken(1L);

		assertThat(token).isNotBlank();
	}

	@Test
	void createRefreshTokenReturnsTokenDifferentFromAccessToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L);
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		assertThat(refreshToken).isNotBlank();
		assertThat(refreshToken).isNotEqualTo(accessToken);
	}

	@Test
	void validateTokenReturnsValidForAccessToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L);

		TokenStatus tokenStatus = jwtTokenProvider.validateToken(accessToken, TokenType.ACCESS);

		assertThat(tokenStatus).isEqualTo(TokenStatus.VALID);
	}

	@Test
	void validateTokenReturnsWrongTypeWhenRefreshTokenIsUsedAsAccessToken() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		TokenStatus tokenStatus = jwtTokenProvider.validateToken(refreshToken, TokenType.ACCESS);

		assertThat(tokenStatus).isEqualTo(TokenStatus.WRONG_TYPE);
	}

	@Test
	void validateTokenReturnsExpiredForExpiredToken() {
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(SECRET, -1000L, 3600000L);
		String expiredToken = expiredTokenProvider.createAccessToken(1L);

		TokenStatus tokenStatus = jwtTokenProvider.validateToken(expiredToken, TokenType.ACCESS);

		assertThat(tokenStatus).isEqualTo(TokenStatus.EXPIRED);
	}

	@Test
	void validateTokenReturnsInvalidForTamperedToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L);

		TokenStatus tokenStatus = jwtTokenProvider.validateToken(accessToken + "tampered", TokenType.ACCESS);

		assertThat(tokenStatus).isEqualTo(TokenStatus.INVALID);
	}

	@Test
	void getUserIdReturnsSubject() {
		String accessToken = jwtTokenProvider.createAccessToken(1L);

		Long userId = jwtTokenProvider.getUserId(accessToken);

		assertThat(userId).isEqualTo(1L);
	}
}
