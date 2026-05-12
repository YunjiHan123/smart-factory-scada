package com.smartfactory.scada.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenPair(
	@Schema(description = "새로 발급된 access token", example = "new-sample-access-token")
	String accessToken,

	@Schema(description = "새로 발급된 refresh token", example = "new-sample-refresh-token")
	String refreshToken
) {
}
