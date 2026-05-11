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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
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
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester");
		given(authService.signup(any(SignupRequest.class)))
			.willReturn(new SignupResponse(1L, request.email(), request.nickname()));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value(request.email()))
			.andExpect(jsonPath("$.nickname").value(request.nickname()));
	}

	@Test
	void signupReturnsValidationErrorWhenEmailIsInvalid() throws Exception {
		SignupRequest request = new SignupRequest("invalid-email", "password1234!", "tester");

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void signupReturnsValidationErrorWhenPasswordIsTooShort() throws Exception {
		SignupRequest request = new SignupRequest("new-user@example.com", "1234", "tester");

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void signupReturnsValidationErrorWhenNicknameIsBlank() throws Exception {
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "");

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
			.willReturn(new LoginResponse(1L, request.email(), "tester", "access-token", "refresh-token"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.email").value(request.email()))
			.andExpect(jsonPath("$.nickname").value("tester"))
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
}
