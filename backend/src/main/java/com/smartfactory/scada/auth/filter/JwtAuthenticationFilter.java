package com.smartfactory.scada.auth.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.auth.jwt.TokenStatus;
import com.smartfactory.scada.auth.jwt.TokenType;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.ErrorResponse;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.mapper.UserMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserMapper userMapper;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return "/api/auth/signup".equals(path)
			|| "/api/auth/login".equals(path)
			|| "/api/auth/refresh".equals(path)
			|| path.startsWith("/swagger-ui/")
			|| "/swagger-ui.html".equals(path)
			|| path.startsWith("/v3/api-docs")
			|| path.startsWith("/webjars/")
			|| "/favicon.ico".equals(path)
			|| "/error".equals(path);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveBearerToken(request);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		TokenStatus tokenStatus = jwtTokenProvider.validateToken(token, TokenType.ACCESS);
		if (tokenStatus != TokenStatus.VALID) {
			writeTokenError(response, tokenStatus);
			return;
		}

		Long userId = jwtTokenProvider.getUserId(token);
		User user = userMapper.findById(userId).orElse(null);
		if (user == null) {
			writeError(response, AuthErrorCode.INVALID_TOKEN);
			return;
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			writeUserStatusError(response, user.getStatus());
			return;
		}

		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getPhone(),
			user.getRole().name(),
			user.getPlantId(),
			user.getStatus().name()
		);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			authenticatedUser,
			null,
			java.util.List.of()
		);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	private String resolveBearerToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	private void writeTokenError(HttpServletResponse response, TokenStatus tokenStatus) throws IOException {
		if (tokenStatus == TokenStatus.EXPIRED) {
			writeError(response, AuthErrorCode.EXPIRED_TOKEN);
			return;
		}

		writeError(response, AuthErrorCode.INVALID_TOKEN);
	}

	private void writeUserStatusError(HttpServletResponse response, UserStatus userStatus) throws IOException {
		if (userStatus == UserStatus.LOCKED) {
			writeError(response, AuthErrorCode.USER_LOCKED);
			return;
		}

		if (userStatus == UserStatus.INACTIVE) {
			writeError(response, AuthErrorCode.USER_INACTIVE);
			return;
		}

		writeError(response, AuthErrorCode.INVALID_TOKEN);
	}

	private void writeError(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.from(errorCode));
	}
}
