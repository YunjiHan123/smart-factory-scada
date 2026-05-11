package com.smartfactory.scada.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.dto.LoginRequest;
import com.smartfactory.scada.auth.dto.LoginResponse;
import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.mapper.UserMapper;

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
			.nickname(request.nickname())
			.build();

		userMapper.insert(user);

		return new SignupResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname()
		);
	}

	public LoginResponse login(LoginRequest request) {
		User user = userMapper.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_LOGIN));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
		}

		String accessToken = jwtTokenProvider.createAccessToken(user.getId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
		refreshTokenService.save(user.getId(), refreshToken);

		return new LoginResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			accessToken,
			refreshToken
		);
	}
}
