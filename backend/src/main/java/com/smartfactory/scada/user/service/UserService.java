package com.smartfactory.scada.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.dto.UserDetailResponse;
import com.smartfactory.scada.user.exception.UserErrorCode;
import com.smartfactory.scada.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;

	@Transactional(readOnly = true)
	public UserDetailResponse getUserDetail(AuthenticatedUser authenticatedUser, Long userId) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}
		validateManagePermission(authenticatedUser);

		return userMapper.findById(userId)
			.map(UserDetailResponse::from)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}

	private void validateManagePermission(AuthenticatedUser authenticatedUser) {
		UserRole role = UserRole.valueOf(authenticatedUser.role());
		if (role == UserRole.ADMIN || role == UserRole.MANAGER) {
			return;
		}

		throw new BusinessException(UserErrorCode.USER_ACCESS_DENIED);
	}
}
