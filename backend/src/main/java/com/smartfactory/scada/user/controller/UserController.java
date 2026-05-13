package com.smartfactory.scada.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.dto.CurrentUserResponse;
import com.smartfactory.scada.user.dto.UserDetailResponse;
import com.smartfactory.scada.user.dto.UserListRequest;
import com.smartfactory.scada.user.dto.UserListResponse;
import com.smartfactory.scada.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

	private final UserService userService;

	@GetMapping("/me")
	@Override
	public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		return new CurrentUserResponse(
			authenticatedUser.userId(),
			authenticatedUser.email(),
			authenticatedUser.name(),
			authenticatedUser.phone(),
			authenticatedUser.role(),
			authenticatedUser.plantId(),
			authenticatedUser.status()
		);
	}

	@GetMapping
	@Override
	public UserListResponse getUsers(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) UserRole role,
		@RequestParam(required = false) UserStatus status,
		@RequestParam(required = false) Long plantId
	) {
		UserListRequest request = UserListRequest.of(page, size, keyword, role, status, plantId);
		return userService.getUsers(authenticatedUser, request);
	}

	@GetMapping("/{userId}")
	@Override
	public UserDetailResponse getUserDetail(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long userId
	) {
		return userService.getUserDetail(authenticatedUser, userId);
	}
}
