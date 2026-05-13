package com.smartfactory.scada.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.dto.TokenPair;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.auth.jwt.TokenStatus;
import com.smartfactory.scada.auth.jwt.TokenType;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.mapper.UserMapper;

import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (userMapper.existsByEmail(request.email())) {
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		User user = User.builder()
			.email(request.email())
			.passwordHash(passwordEncoder.encode(request.password()))
			.name(request.name())
			.phone(request.phone())
			.role(UserRole.VIEWER)
			.plantId(request.plantId())
			.status(UserStatus.ACTIVE)
			.build();

		userMapper.insert(user);

		return new SignupResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getPhone(),
			user.getRole().name(),
			user.getPlantId(),
			user.getStatus().name()
		);
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		User user = userMapper.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_LOGIN));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
		}
		validateActiveUser(user);

		String accessToken = jwtTokenProvider.createAccessToken(user.getId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
		refreshTokenService.save(user.getId(), refreshToken);
		userMapper.updateLastLoginAt(user.getId());

		return new LoginResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getRole().name(),
			user.getPlantId(),
			accessToken,
			refreshToken
		);
	}

	public TokenPair refresh(String accessToken, String refreshToken) {
		Long accessUserId = getAccessTokenUserId(accessToken);
		validateRefreshToken(refreshToken);
		Long refreshUserId = jwtTokenProvider.getUserId(refreshToken);

		if (!accessUserId.equals(refreshUserId)) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		User user = userMapper.findById(refreshUserId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
		validateActiveUser(user);

		if (!refreshTokenService.exists(refreshUserId)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
		}

		if (!refreshTokenService.matches(refreshUserId, refreshToken)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(refreshUserId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(refreshUserId);
		refreshTokenService.save(refreshUserId, newRefreshToken);

		return new TokenPair(newAccessToken, newRefreshToken);
	}

	public void logout(AuthenticatedUser authenticatedUser) {
		refreshTokenService.delete(authenticatedUser.userId());
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() == UserStatus.ACTIVE) {
			return;
		}

		if (user.getStatus() == UserStatus.LOCKED) {
			throw new BusinessException(AuthErrorCode.USER_LOCKED);
		}

		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new BusinessException(AuthErrorCode.USER_INACTIVE);
		}

		throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
	}

	private Long getAccessTokenUserId(String accessToken) {
		try {
			return jwtTokenProvider.getUserIdAllowExpired(accessToken, TokenType.ACCESS);
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	private void validateRefreshToken(String refreshToken) {
		TokenStatus tokenStatus = jwtTokenProvider.validateToken(refreshToken, TokenType.REFRESH);
		if (tokenStatus == TokenStatus.VALID) {
			return;
		}

		if (tokenStatus == TokenStatus.EXPIRED) {
			throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
		}

		throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
	}
}
