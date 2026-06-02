package com.smartfactory.scada.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		SecurityScheme bearerAuth = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT");

		return new OpenAPI()
			.info(apiInfo())
			.components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuth));
	}

	private Info apiInfo() {
		return new Info()
			.title("Energy Integrated Control System API")
			.description("""
				에너지 통합 관제 시스템 백엔드 API 문서입니다.

				## 프론트 연동 공통 정책

				### 공개 API
				- `POST /api/auth/signup`: 회원가입 / 사용자 등록
				- `POST /api/auth/login`: 로그인 및 토큰 발급
				- `POST /api/auth/refresh`: access token / refresh token 재발급

				### 보호 API
				- `POST /api/auth/logout`
				- `/api/users/**`

				### JWT 사용 방법
				- Swagger UI의 `Authorize` 버튼에는 로그인 응답의 `accessToken` 원문만 입력합니다.
				- 실제 HTTP 요청에서는 `Authorization: Bearer {accessToken}` 형식으로 보냅니다.
				- refresh API는 예외적으로 `Authorization: Bearer {accessToken}`와 `X-Refresh-Token: {refreshToken}` 헤더를 직접 함께 보냅니다.
				- refresh 성공 후에는 응답으로 받은 새 `accessToken`, `refreshToken`을 기존 토큰 대신 사용합니다.

				### 사용자 정책
				- 회원가입 / 사용자 등록 시 서버가 `role=VIEWER`, `status=ACTIVE`로 저장합니다.
				- 로그인 실패는 계정 존재 여부를 숨기기 위해 `INVALID_LOGIN`으로 통일합니다.
				- `ACTIVE` 사용자만 로그인과 보호 API 접근이 가능합니다.
				- `INACTIVE`, `LOCKED` 사용자는 JWT가 유효해도 접근이 차단됩니다.
				- 사용자 목록/상세/수정은 `ADMIN` 또는 `MANAGER`만 가능합니다.
				- `MANAGER`는 `ADMIN` 사용자를 수정할 수 없고, 다른 사용자를 `ADMIN`으로 승격할 수 없습니다.

				### 사용자 목록/수정 정책
				- 사용자 목록의 `page`는 0부터 시작합니다.
				- 사용자 목록의 `size` 기본값은 20, 최대값은 100입니다.
				- 사용자 수정 PATCH에서 누락된 필드 또는 `null` 필드는 수정하지 않습니다.
				- v1에서는 PATCH 요청으로 `plantId`, `note` 같은 필드를 `null`로 비우는 기능을 제공하지 않습니다.

				### 공통 에러 응답
				모든 비즈니스/검증 에러는 아래 형태를 사용합니다.

				```json
				{
				  "code": "VALIDATION_ERROR",
				  "message": "요청 값이 올바르지 않습니다."
				}
				```
				""")
			.version("v1");
	}
}
