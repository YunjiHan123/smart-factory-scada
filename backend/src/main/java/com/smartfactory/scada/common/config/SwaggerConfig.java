package com.smartfactory.scada.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
			.components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuth))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

	private Info apiInfo() {
		return new Info()
			.title("Energy Integrated Control System API")
			.description("에너지 통합 관제 시스템 백엔드 API 문서")
			.version("v1");
	}
}
