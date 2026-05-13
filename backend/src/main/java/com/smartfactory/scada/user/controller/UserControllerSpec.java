package com.smartfactory.scada.user.controller;

import org.springframework.http.MediaType;

import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.user.dto.CurrentUserResponse;
import com.smartfactory.scada.user.dto.UserDetailResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사용자 API", description = "현재 로그인한 사용자 정보와 사용자 관리 API")
public interface UserControllerSpec {

	@Operation(
		summary = "내 정보 조회",
		description = """
			Authorization 헤더의 access token을 검증한 뒤 현재 로그인한 사용자 정보를 반환합니다.<br>
			Swagger에서는 로그인 API로 받은 accessToken을 Authorize 버튼에 먼저 입력한 뒤 호출해주세요.<br>
			INACTIVE 또는 LOCKED 상태의 사용자는 JWT가 유효하더라도 접근할 수 없습니다.
			"""
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "내 정보 조회 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = CurrentUserResponse.class)
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보 누락 또는 유효하지 않은 access token", content = @Content),
		@ApiResponse(responseCode = "403", description = "비활성화 또는 잠긴 사용자", content = @Content)
	})
	CurrentUserResponse me(@Parameter(hidden = true) AuthenticatedUser authenticatedUser);

	@Operation(
		summary = "사용자 상세 조회",
		description = """
			사용자 관리 화면에서 특정 사용자의 상세 정보를 조회합니다.<br>
			ADMIN 또는 MANAGER 권한을 가진 사용자만 호출할 수 있습니다.<br>
			본인 정보 조회가 필요하면 GET /api/users/me API를 사용해주세요.
			"""
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "사용자 상세 조회 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = UserDetailResponse.class)
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보 누락 또는 유효하지 않은 access token", content = @Content),
		@ApiResponse(responseCode = "403", description = "사용자 관리 권한 없음", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
	})
	UserDetailResponse getUserDetail(
		@Parameter(hidden = true) AuthenticatedUser authenticatedUser,

		@Parameter(description = "조회할 사용자 ID", example = "1", required = true)
		Long userId
	);
}
