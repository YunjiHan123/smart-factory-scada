package com.smartfactory.scada.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.dto.UserListRequest;
import com.smartfactory.scada.user.dto.UserUpdateRequest;
import com.smartfactory.scada.user.mapper.UserMapper;
import com.smartfactory.scada.user.service.UserService;

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
	void listReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/users"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void listReturnsUsersWhenRequesterIsAdmin() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));
		given(userMapper.countUsers(any(UserListRequest.class))).willReturn(1L);
		given(userMapper.findUsers(any(UserListRequest.class))).willReturn(List.of(user(2L, UserRole.OPERATOR)));

		mockMvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
				.param("page", "1")
				.param("size", "10")
				.param("keyword", "operator")
				.param("role", "OPERATOR")
				.param("status", "ACTIVE")
				.param("plantId", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalCount").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.items[0].userId").value(2L))
			.andExpect(jsonPath("$.items[0].email").value("user2@example.com"))
			.andExpect(jsonPath("$.items[0].role").value("OPERATOR"))
			.andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
			.andExpect(jsonPath("$.items[0].note").doesNotExist());

		ArgumentCaptor<UserListRequest> requestCaptor = ArgumentCaptor.forClass(UserListRequest.class);
		then(userMapper).should().countUsers(requestCaptor.capture());
		UserListRequest request = requestCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(request.page()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(request.size()).isEqualTo(10);
		org.assertj.core.api.Assertions.assertThat(request.offset()).isEqualTo(10);
		org.assertj.core.api.Assertions.assertThat(request.keyword()).isEqualTo("operator");
		org.assertj.core.api.Assertions.assertThat(request.role()).isEqualTo(UserRole.OPERATOR);
		org.assertj.core.api.Assertions.assertThat(request.status()).isEqualTo(UserStatus.ACTIVE);
		org.assertj.core.api.Assertions.assertThat(request.plantId()).isEqualTo(1L);
	}

	@Test
	void listReturnsForbiddenWhenRequesterIsViewer() throws Exception {
		given(jwtTokenProvider.validateToken("viewer-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("viewer-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.VIEWER)));

		mockMvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
	}

	@Test
	void listReturnsBadRequestWhenEnumQueryIsInvalid() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));

		mockMvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
				.param("role", "UNKNOWN"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(patch("/api/users/2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"updated\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void updateReturnsUserWhenRequesterIsAdmin() throws Exception {
		User targetUser = user(2L, UserRole.OPERATOR);
		User updatedUser = user(2L, UserRole.MANAGER);
		updatedUser.setName("updated");
		updatedUser.setPhone("010-9999-8888");
		updatedUser.setPlantId(2L);
		updatedUser.setNote("updated note");

		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));
		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser), Optional.of(updatedUser));
		given(userMapper.updateUser(eq(2L), any(UserUpdateRequest.class))).willReturn(1);

		mockMvc.perform(patch("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "updated",
					  "phone": "010-9999-8888",
					  "role": "MANAGER",
					  "plantId": 2,
					  "status": "ACTIVE",
					  "note": "updated note"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(2L))
			.andExpect(jsonPath("$.name").value("updated"))
			.andExpect(jsonPath("$.phone").value("010-9999-8888"))
			.andExpect(jsonPath("$.role").value("MANAGER"))
			.andExpect(jsonPath("$.plantId").value(2L))
			.andExpect(jsonPath("$.note").value("updated note"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		then(userMapper).should().updateUser(eq(2L), any(UserUpdateRequest.class));
	}

	@Test
	void updateReturnsForbiddenWhenRequesterIsViewer() throws Exception {
		given(jwtTokenProvider.validateToken("viewer-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("viewer-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.VIEWER)));

		mockMvc.perform(patch("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"updated\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
	}

	@Test
	void updateReturnsBadRequestWhenRequestHasNoUpdates() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));

		mockMvc.perform(patch("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateReturnsBadRequestWhenEnumBodyIsInvalid() throws Exception {
		given(jwtTokenProvider.validateToken("admin-token", TokenType.ACCESS)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("admin-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(user(1L, UserRole.ADMIN)));

		mockMvc.perform(patch("/api/users/2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"UNKNOWN\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
