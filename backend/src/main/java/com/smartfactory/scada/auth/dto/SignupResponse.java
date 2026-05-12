package com.smartfactory.scada.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupResponse(
	@Schema(description = "가입된 사용자 ID", example = "1")
	Long userId,

	@Schema(description = "가입된 사용자 이메일", example = "new-user@example.com")
	String email,

	@Schema(description = "가입된 사용자 닉네임", example = "홍길동")
	String nickname
) {
}
