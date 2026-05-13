package com.smartfactory.scada.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupResponse(
	@Schema(description = "등록된 사용자 ID", example = "1")
	Long userId,

	@Schema(description = "등록된 사용자 이메일", example = "new-user@example.com")
	String email,

	@Schema(description = "등록된 사용자 이름", example = "조혜원")
	String name,

	@Schema(description = "등록된 사용자 전화번호", example = "010-0000-0000")
	String phone,

	@Schema(description = "서버에서 기본 저장한 사용자 권한", example = "VIEWER")
	String role,

	@Schema(description = "등록된 사용자의 사업장 ID", example = "1")
	Long plantId,

	@Schema(description = "서버에서 기본 저장한 사용자 상태", example = "ACTIVE")
	String status
) {
}
