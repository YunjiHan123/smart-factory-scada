package com.smartfactory.scada.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.dto.CurrentUserResponse;
import com.smartfactory.scada.user.dto.UserDetailResponse;
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

	@GetMapping("/{userId}")
	@Override
	public UserDetailResponse getUserDetail(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long userId
	) {
		return userService.getUserDetail(authenticatedUser, userId);
	}
}
