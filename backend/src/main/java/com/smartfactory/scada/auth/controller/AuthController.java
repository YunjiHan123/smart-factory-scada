package com.smartfactory.scada.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.dto.TokenPair;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.service.AuthService;
import com.smartfactory.scada.common.exception.BusinessException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

	private final AuthService authService;

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public TokenPair refresh(
		@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
		@RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken
	) {
		String accessToken = resolveBearerToken(authorizationHeader);
		if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(refreshToken)) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		return authService.refresh(accessToken, refreshToken);
	}

	private String resolveBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
