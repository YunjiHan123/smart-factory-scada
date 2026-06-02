package com.smartfactory.scada.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.smartfactory.scada.auth.controller.AuthControllerSpec;
import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.user.controller.UserControllerSpec;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.OpenAPI;

class SwaggerConfigTest {

	@Test
	void openApiRegistersBearerSchemeWithoutGlobalSecurityRequirement() {
		OpenAPI openAPI = new SwaggerConfig().openAPI();

		assertThat(openAPI.getSecurity()).isNullOrEmpty();
		assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
		assertThat(openAPI.getInfo().getDescription())
			.contains("공개 API")
			.contains("보호 API")
			.contains("공통 에러 응답")
			.contains("Authorization: Bearer {accessToken}")
			.contains("X-Refresh-Token: {refreshToken}");
	}

	@Test
	void authSwaggerMarksOnlyLogoutAsBearerProtected() throws NoSuchMethodException {
		Method signup = AuthControllerSpec.class.getMethod("signup", SignupRequest.class);
		Method login = AuthControllerSpec.class.getMethod("login", LoginRequest.class);
		Method refresh = AuthControllerSpec.class.getMethod("refresh", String.class, String.class);
		Method logout = AuthControllerSpec.class.getMethod("logout", AuthenticatedUser.class);

		assertThat(signup.getAnnotation(SecurityRequirement.class)).isNull();
		assertThat(login.getAnnotation(SecurityRequirement.class)).isNull();
		assertThat(refresh.getAnnotation(SecurityRequirement.class)).isNull();
		assertThat(logout.getAnnotation(SecurityRequirement.class).name()).isEqualTo("bearerAuth");
	}

	@Test
	void userSwaggerIsBearerProtectedAtControllerLevel() {
		assertThat(UserControllerSpec.class.getAnnotation(SecurityRequirement.class).name()).isEqualTo("bearerAuth");
	}
}
