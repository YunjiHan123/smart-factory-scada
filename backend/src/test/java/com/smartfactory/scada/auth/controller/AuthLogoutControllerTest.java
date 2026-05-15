package com.smartfactory.scada.auth.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smartfactory.scada.auth.filter.JwtAuthenticationFilter;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.auth.jwt.TokenStatus;
import com.smartfactory.scada.auth.jwt.TokenType;
import com.smartfactory.scada.auth.security.JwtAuthenticationEntryPoint;
import com.smartfactory.scada.auth.service.AuthService;
import com.smartfactory.scada.common.config.SecurityConfig;
import com.smartfactory.scada.common.exception.GlobalExceptionHandler;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.mapper.UserMapper;

@WebMvcTest(AuthController.class)
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	JwtAuthenticationEntryPoint.class,
	GlobalExceptionHandler.class
})
class AuthLogoutControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserMapper userMapper;

	@Test
	void logoutReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void logoutReturnsNoContentWhenAccessTokenIsValid() throws Exception {
		given(jwtTokenProvider.validateToken("access-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("access-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("login@example.com")
			.name("tester")
			.phone("010-0000-0000")
			.role(UserRole.VIEWER)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.build()));

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isNoContent());

		then(authService).should().logout(eq(new com.smartfactory.scada.auth.security.AuthenticatedUser(
			1L,
			"login@example.com",
			"tester",
			"010-0000-0000",
			UserRole.VIEWER.name(),
			1L,
			UserStatus.ACTIVE.name()
		)));
	}

	@Test
	void logoutReturnsUnauthorizedWhenRefreshTokenIsUsed() throws Exception {
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.ACCESS)).willReturn(TokenStatus.WRONG_TYPE);

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void logoutReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
		given(jwtTokenProvider.validateToken("invalid-token", TokenType.ACCESS)).willReturn(TokenStatus.INVALID);

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}
}
