package com.smartfactory.scada.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.dto.TokenPair;
import com.smartfactory.scada.auth.filter.JwtAuthenticationFilter;
import com.smartfactory.scada.auth.security.JwtAuthenticationEntryPoint;
import com.smartfactory.scada.auth.service.AuthService;
import com.smartfactory.scada.common.exception.GlobalExceptionHandler;

@WebMvcTest({AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Test
	void signupReturnsCreatedStatus() throws Exception {
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester", "010-0000-0000", 1L);
		given(authService.signup(any(SignupRequest.class)))
			.willReturn(new SignupResponse(1L, request.email(), request.name(), request.phone(), "VIEWER", request.plantId(), "ACTIVE"));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value(request.email()))
			.andExpect(jsonPath("$.name").value(request.name()))
			.andExpect(jsonPath("$.phone").value(request.phone()))
			.andExpect(jsonPath("$.role").value("VIEWER"))
			.andExpect(jsonPath("$.plantId").value(1L))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void signupReturnsValidationErrorWhenEmailIsInvalid() throws Exception {
		SignupRequest request = new SignupRequest("invalid-email", "password1234!", "tester", "010-0000-0000", 1L);

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void signupReturnsValidationErrorWhenPasswordIsTooShort() throws Exception {
		SignupRequest request = new SignupRequest("new-user@example.com", "1234", "tester", "010-0000-0000", 1L);

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void signupReturnsValidationErrorWhenNameIsBlank() throws Exception {
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "", "010-0000-0000", 1L);

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void loginReturnsOkStatus() throws Exception {
		LoginRequest request = new LoginRequest("login@example.com", "password1234!");
		given(authService.login(any(LoginRequest.class)))
			.willReturn(new LoginResponse(1L, request.email(), "tester", "VIEWER", 1L, "access-token", "refresh-token"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value(request.email()))
			.andExpect(jsonPath("$.name").value("tester"))
			.andExpect(jsonPath("$.role").value("VIEWER"))
			.andExpect(jsonPath("$.plantId").value(1L))
			.andExpect(jsonPath("$.accessToken").value("access-token"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void loginReturnsValidationErrorWhenEmailIsInvalid() throws Exception {
		LoginRequest request = new LoginRequest("invalid-email", "password1234!");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void loginReturnsValidationErrorWhenPasswordIsBlank() throws Exception {
		LoginRequest request = new LoginRequest("login@example.com", "");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void refreshReturnsTokenPair() throws Exception {
		given(authService.refresh("access-token", "refresh-token"))
			.willReturn(new TokenPair("new-access-token", "new-refresh-token"));

		mockMvc.perform(post("/api/auth/refresh")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.header("X-Refresh-Token", "refresh-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
	}

	@Test
	void refreshReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.header("X-Refresh-Token", "refresh-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void refreshReturnsUnauthorizedWhenRefreshTokenHeaderIsMissing() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}
}
