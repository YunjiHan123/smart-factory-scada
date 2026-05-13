package com.smartfactory.scada.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.dto.UserDetailResponse;
import com.smartfactory.scada.user.exception.UserErrorCode;
import com.smartfactory.scada.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private UserService userService;

	@Test
	void getUserDetailReturnsUserWhenRequesterIsAdmin() {
		User targetUser = targetUser();
		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser));

		UserDetailResponse response = userService.getUserDetail(authenticatedUser(UserRole.ADMIN), 2L);

		assertThat(response.userId()).isEqualTo(targetUser.getId());
		assertThat(response.email()).isEqualTo(targetUser.getEmail());
		assertThat(response.name()).isEqualTo(targetUser.getName());
		assertThat(response.phone()).isEqualTo(targetUser.getPhone());
		assertThat(response.role()).isEqualTo(targetUser.getRole().name());
		assertThat(response.plantId()).isEqualTo(targetUser.getPlantId());
		assertThat(response.status()).isEqualTo(targetUser.getStatus().name());
		assertThat(response.note()).isEqualTo(targetUser.getNote());
		assertThat(response.lastLoginAt()).isEqualTo(targetUser.getLastLoginAt());
	}

	@Test
	void getUserDetailReturnsUserWhenRequesterIsManager() {
		User targetUser = targetUser();
		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser));

		UserDetailResponse response = userService.getUserDetail(authenticatedUser(UserRole.MANAGER), 2L);

		assertThat(response.userId()).isEqualTo(targetUser.getId());
	}

	@Test
	void getUserDetailThrowsAccessDeniedWhenRequesterIsViewer() {
		assertThatThrownBy(() -> userService.getUserDetail(authenticatedUser(UserRole.VIEWER), 2L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);
	}

	@Test
	void getUserDetailThrowsAccessDeniedWhenRequesterIsOperator() {
		assertThatThrownBy(() -> userService.getUserDetail(authenticatedUser(UserRole.OPERATOR), 2L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);
	}

	@Test
	void getUserDetailThrowsNotFoundWhenUserDoesNotExist() {
		given(userMapper.findById(2L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUserDetail(authenticatedUser(UserRole.ADMIN), 2L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
			);
	}

	@Test
	void userDetailResponseDoesNotExposePasswordHash() {
		assertThat(UserDetailResponse.class.getRecordComponents())
			.extracting("name")
			.doesNotContain("passwordHash");
	}

	private AuthenticatedUser authenticatedUser(UserRole role) {
		return new AuthenticatedUser(
			1L,
			"admin@example.com",
			"admin",
			"010-0000-0000",
			role.name(),
			1L,
			UserStatus.ACTIVE.name()
		);
	}

	private User targetUser() {
		return User.builder()
			.id(2L)
			.email("operator@example.com")
			.passwordHash("encoded-password")
			.name("operator")
			.phone("010-1111-2222")
			.role(UserRole.OPERATOR)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.note("광명 공장 담당자")
			.lastLoginAt(LocalDateTime.of(2026, 5, 13, 10, 20, 30))
			.createdAt(LocalDateTime.of(2026, 5, 13, 9, 0, 0))
			.updatedAt(LocalDateTime.of(2026, 5, 13, 10, 0, 0))
			.build();
	}
}
