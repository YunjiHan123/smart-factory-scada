package com.smartfactory.scada.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smartfactory.scada.auth.filter.JwtAuthenticationFilter;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.auth.security.JwtAuthenticationEntryPoint;
import com.smartfactory.scada.common.exception.GlobalExceptionHandler;
import com.smartfactory.scada.user.controller.UserController;
import com.smartfactory.scada.user.mapper.UserMapper;

@WebMvcTest(UserController.class)
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	JwtAuthenticationEntryPoint.class,
	GlobalExceptionHandler.class
})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserMapper userMapper;

	@Test
	void swaggerApiDocsIsNotBlockedByAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(result -> assertThat(result.getResponse().getStatus())
				.isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
	}

	@Test
	void swaggerUiIsNotBlockedByAuthentication() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(result -> assertThat(result.getResponse().getStatus())
				.isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
	}

	@Test
	void protectedApiStillRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}
}
