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
import com.smartfactory.scada.user.service.UserService;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.mapper.UserMapper;

@WebMvcTest(UserController.class)
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	JwtAuthenticationEntryPoint.class,
	GlobalExceptionHandler.class,
	UserService.class
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
			.name("tester")
			.phone("010-0000-0000")
			.role(UserRole.VIEWER)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.build()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value("login@example.com"))
			.andExpect(jsonPath("$.name").value("tester"))
			.andExpect(jsonPath("$.phone").value("010-0000-0000"))
			.andExpect(jsonPath("$.role").value("VIEWER"))
			.andExpect(jsonPath("$.plantId").value(1L))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void meReturnsForbiddenWhenUserIsInactive() throws Exception {
		given(jwtTokenProvider.validateToken("access-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("access-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("inactive@example.com")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.INACTIVE)
			.build()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("USER_INACTIVE"));
	}

	@Test
	void meReturnsForbiddenWhenUserIsLocked() throws Exception {
		given(jwtTokenProvider.validateToken("access-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("access-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("locked@example.com")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.LOCKED)
			.build()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("USER_LOCKED"));
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

	@Test
	void detailReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/users/2"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void detailReturnsUserWhenRequesterIsAdmin() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));
		given(userMapper.findById(2L)).willReturn(Optional.of(user(2L, UserRole.OPERATOR)));

		mockMvc.perform(get("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(2L))
			.andExpect(jsonPath("$.email").value("user2@example.com"))
			.andExpect(jsonPath("$.name").value("user2"))
			.andExpect(jsonPath("$.phone").value("010-0000-0002"))
			.andExpect(jsonPath("$.role").value("OPERATOR"))
			.andExpect(jsonPath("$.plantId").value(1L))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.note").value("note"));
	}

	@Test
	void detailReturnsForbiddenWhenRequesterIsViewer() throws Exception {
		given(jwtTokenProvider.validateToken("viewer-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("viewer-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.VIEWER)));

		mockMvc.perform(get("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
	}

	@Test
	void detailReturnsNotFoundWhenTargetUserDoesNotExist() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));
		given(userMapper.findById(999L)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/users/999")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	private User user(Long userId, UserRole role) {
		return User.builder()
			.id(userId)
			.email("user" + userId + "@example.com")
			.name("user" + userId)
			.phone("010-0000-000" + userId)
			.role(role)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.note("note")
			.build();
	}
}
