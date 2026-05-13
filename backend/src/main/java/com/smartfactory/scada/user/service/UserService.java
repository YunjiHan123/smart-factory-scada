package com.smartfactory.scada.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.dto.UserDetailResponse;
import com.smartfactory.scada.user.dto.UserListItemResponse;
import com.smartfactory.scada.user.dto.UserListRequest;
import com.smartfactory.scada.user.dto.UserListResponse;
import com.smartfactory.scada.user.dto.UserUpdateRequest;
import com.smartfactory.scada.user.exception.UserErrorCode;
import com.smartfactory.scada.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;

	@Transactional
	public UserDetailResponse updateUser(
		AuthenticatedUser authenticatedUser,
		Long userId,
		UserUpdateRequest request
	) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}
		validateManagePermission(authenticatedUser);
		validateUpdateRequest(request);

		User targetUser = userMapper.findById(userId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		validateManagerUpdatePolicy(authenticatedUser, targetUser, request);

		userMapper.updateUser(userId, request);

		return userMapper.findById(userId)
			.map(UserDetailResponse::from)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public UserListResponse getUsers(AuthenticatedUser authenticatedUser, UserListRequest request) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}
		validateManagePermission(authenticatedUser);

		long totalCount = userMapper.countUsers(request);
		var items = userMapper.findUsers(request)
			.stream()
			.map(UserListItemResponse::from)
			.toList();

		return UserListResponse.of(items, request, totalCount);
	}

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

	private void validateUpdateRequest(UserUpdateRequest request) {
		if (request == null || !request.hasUpdates()) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}
	}

	private void validateManagerUpdatePolicy(
		AuthenticatedUser authenticatedUser,
		User targetUser,
		UserUpdateRequest request
	) {
		UserRole requesterRole = UserRole.valueOf(authenticatedUser.role());
		if (requesterRole != UserRole.MANAGER) {
			return;
		}

		if (targetUser.getRole() == UserRole.ADMIN || request.role() == UserRole.ADMIN) {
			throw new BusinessException(UserErrorCode.USER_ACCESS_DENIED);
		}
	}
}
