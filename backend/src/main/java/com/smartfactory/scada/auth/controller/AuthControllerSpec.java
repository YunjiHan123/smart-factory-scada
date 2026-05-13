package com.smartfactory.scada.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.dto.TokenPair;
import com.smartfactory.scada.auth.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 재발급, 로그아웃 API")
public interface AuthControllerSpec {

	@Operation(
		summary = "회원가입 / 사용자 등록",
		description = """
			이메일, 비밀번호, 이름, 전화번호, 사업장 ID로 새 사용자를 등록합니다.<br>
			사용자 관리 화면의 사용자 등록도 별도 POST /api/users를 만들지 않고 이 API를 사용합니다.<br>
			비밀번호는 서버에서 BCrypt 해시로 변환해 저장하고, 응답에는 비밀번호나 passwordHash를 포함하지 않습니다.<br>
			role은 VIEWER, status는 ACTIVE로 서버에서 기본 저장합니다.<br>
			이미 가입된 이메일이면 409 상태코드와 함께 아래와 같은 에러 코드가 반환됩니다.
			```json
			{
			  "code": "EMAIL_ALREADY_EXISTS",
			  "message": "이미 가입된 이메일입니다."
			}
			```
			"""
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "회원가입 / 사용자 등록 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = SignupResponse.class)
			)
		),
		@ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
		@ApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = @Content)
	})
	SignupResponse signup(SignupRequest request);

	@Operation(
		summary = "로그인 및 토큰 발급",
		description = """
			이메일과 비밀번호를 검증한 뒤 access token과 refresh token을 발급합니다.<br>
			Swagger에서 보호 API를 테스트하려면 로그인 응답의 accessToken을 복사한 뒤, 화면 오른쪽 위 Authorize 버튼에 입력해주세요.<br>
			INACTIVE 또는 LOCKED 상태의 사용자는 로그인할 수 없습니다.<br>
			로그인 실패 시 이메일 존재 여부를 노출하지 않기 위해 없는 이메일과 틀린 비밀번호를 모두 INVALID_LOGIN으로 응답합니다.
			```json
			{
			  "code": "INVALID_LOGIN",
			  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
			}
			```
			"""
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "로그인 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = LoginResponse.class)
			)
		),
		@ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
		@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content),
		@ApiResponse(responseCode = "403", description = "비활성화 또는 잠긴 사용자", content = @Content)
	})
	LoginResponse login(LoginRequest request);

	@Operation(
		summary = "토큰 재발급",
		description = """
			access token이 만료됐거나 만료 직전일 때 새 access token과 refresh token을 발급받는 API입니다.<br>
			이 API는 Swagger Authorize 버튼의 전역 인증을 사용하지 않고, 아래 두 헤더를 직접 입력해서 테스트합니다.<br>
			Authorization 헤더에는 Bearer 접두사가 붙은 access token을 넣고, X-Refresh-Token 헤더에는 refresh token을 넣어주세요.<br>
			재발급에 성공하면 응답으로 받은 새 accessToken과 refreshToken을 기존 토큰 대신 사용해야 합니다.
			```http
			Authorization: Bearer {accessToken}
			X-Refresh-Token: {refreshToken}
			```
			사용자 상태가 ACTIVE가 아니거나, 토큰이 유효하지 않거나, Redis에 저장된 refresh token 해시와 일치하지 않으면 실패합니다.
			```json
			{
			  "code": "REFRESH_TOKEN_MISMATCH",
			  "message": "리프레시 토큰이 일치하지 않습니다."
			}
			```
			"""
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "토큰 재발급 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = TokenPair.class)
			)
		),
		@ApiResponse(responseCode = "401", description = "토큰 누락, 만료, 불일치 또는 유효하지 않은 토큰", content = @Content),
		@ApiResponse(responseCode = "403", description = "비활성화 또는 잠긴 사용자", content = @Content)
	})
	TokenPair refresh(
		@Parameter(
			name = HttpHeaders.AUTHORIZATION,
			description = "Bearer 접두사가 붙은 access token. 예: Bearer {accessToken}",
			in = ParameterIn.HEADER,
			required = true
		)
		String authorizationHeader,

		@Parameter(
			name = "X-Refresh-Token",
			description = "로그인 또는 직전 재발급 때 받은 refresh token",
			in = ParameterIn.HEADER,
			required = true
		)
		String refreshToken
	);

	@Operation(
		summary = "로그아웃",
		description = """
			현재 로그인한 사용자의 Redis refresh token을 삭제합니다.<br>
			Swagger에서 테스트하려면 로그인 응답의 accessToken을 Authorize 버튼에 입력한 뒤 호출합니다.<br>
			로그아웃 후에는 기존 refresh token으로 토큰 재발급을 할 수 없습니다.<br>
			다만 access token은 stateless JWT라서 서버가 즉시 삭제하지 않으며, 남은 만료 시간 동안 이론상 유효할 수 있습니다.
			"""
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "로그아웃 성공", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 정보 누락 또는 유효하지 않은 access token", content = @Content),
		@ApiResponse(responseCode = "403", description = "비활성화 또는 잠긴 사용자", content = @Content)
	})
	void logout(@Parameter(hidden = true) AuthenticatedUser authenticatedUser);
}
