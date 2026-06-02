package com.smartfactory.scada.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
	@Schema(description = "로그인한 사용자 ID", example = "1")
	Long userId,

	@Schema(description = "로그인한 사용자 이메일", example = "new-user@example.com")
	String email,

	@Schema(description = "로그인한 사용자 이름", example = "조혜원")
	String name,

	@Schema(description = "로그인한 사용자 권한", example = "VIEWER")
	String role,

	@Schema(description = "로그인한 사용자의 사업장 ID", example = "1")
	Long plantId,

	@Schema(description = "API 인증에 사용하는 access token", example = "sample-access-token")
	String accessToken,

	@Schema(description = "access token 재발급에 사용하는 refresh token", example = "sample-refresh-token")
	String refreshToken
) {
}
