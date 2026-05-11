package com.smartfactory.scada.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.dto.SignupRequest;
import com.smartfactory.scada.auth.dto.SignupResponse;
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

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
}
