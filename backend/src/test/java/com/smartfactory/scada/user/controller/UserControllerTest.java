package com.smartfactory.scada.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.smartfactory.scada.common.config.SecurityConfig;
import com.smartfactory.scada.common.exception.GlobalExceptionHandler;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.mapper.UserMapper;

@WebMvcTest(UserController.class)
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	JwtAuthenticationEntryPoint.class,
	GlobalExceptionHandler.class
})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserMapper userMapper;

	@Test
	void meReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void meReturnsCurrentUserWhenAccessTokenIsValid() throws Exception {
		given(jwtTokenProvider.validateToken("access-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("access-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("login@example.com")
			.nickname("tester")
			.build()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value("login@example.com"))
			.andExpect(jsonPath("$.nickname").value("tester"));
	}

	@Test
	void meReturnsUnauthorizedWhenRefreshTokenIsUsed() throws Exception {
		given(jwtTokenProvider.validateToken(eq("refresh-token"), eq(TokenType.ACCESS)))
			.willReturn(TokenStatus.WRONG_TYPE);

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void meReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
		given(jwtTokenProvider.validateToken(eq("invalid-token"), eq(TokenType.ACCESS)))
			.willReturn(TokenStatus.INVALID);

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void meReturnsUnauthorizedWhenTokenIsExpired() throws Exception {
		given(jwtTokenProvider.validateToken(eq("expired-token"), eq(TokenType.ACCESS)))
			.willReturn(TokenStatus.EXPIRED);

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("EXPIRED_TOKEN"));
	}

	@Test
	void meReturnsUnauthorizedWhenUserDoesNotExist() throws Exception {
		given(jwtTokenProvider.validateToken("access-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("access-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}
}
