package com.smartfactory.scada.user.dto;

import java.time.LocalDateTime;

import com.smartfactory.scada.user.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserListItemResponse(
	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "사용자 이메일", example = "operator@example.com")
	String email,

	@Schema(description = "사용자 이름", example = "조혜원")
	String name,

	@Schema(description = "사용자 전화번호", example = "010-0000-0000")
	String phone,

	@Schema(description = "사용자 권한", example = "VIEWER")
	String role,

	@Schema(description = "사용자가 소속된 사업장 ID", example = "1")
	Long plantId,

	@Schema(description = "사용자 상태", example = "ACTIVE")
	String status,

	@Schema(description = "마지막 로그인 일시", example = "2026-05-13T10:20:30")
	LocalDateTime lastLoginAt,

	@Schema(description = "사용자 생성 일시", example = "2026-05-13T09:00:00")
	LocalDateTime createdAt
) {

	public static UserListItemResponse from(User user) {
		return new UserListItemResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getPhone(),
			user.getRole().name(),
			user.getPlantId(),
			user.getStatus().name(),
			user.getLastLoginAt(),
			user.getCreatedAt()
		);
	}
}
