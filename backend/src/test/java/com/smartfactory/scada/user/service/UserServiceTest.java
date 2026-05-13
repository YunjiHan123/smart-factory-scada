package com.smartfactory.scada.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.security.AuthenticatedUser;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.common.exception.CommonErrorCode;
import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;
import com.smartfactory.scada.user.dto.UserDetailResponse;
import com.smartfactory.scada.user.dto.UserListItemResponse;
import com.smartfactory.scada.user.dto.UserListRequest;
import com.smartfactory.scada.user.dto.UserListResponse;
import com.smartfactory.scada.user.dto.UserUpdateRequest;
import com.smartfactory.scada.user.exception.UserErrorCode;
import com.smartfactory.scada.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private UserService userService;

	@Test
	void updateUserReturnsUpdatedDetailWhenRequesterIsAdmin() {
		User targetUser = user(2L, UserRole.OPERATOR);
		User updatedUser = user(2L, UserRole.MANAGER);
		updatedUser.setName("updated");
		updatedUser.setPhone("010-9999-8888");
		updatedUser.setNote("updated note");

		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser), Optional.of(updatedUser));
		UserUpdateRequest request = new UserUpdateRequest(
			"updated",
			"010-9999-8888",
			UserRole.MANAGER,
			2L,
			UserStatus.ACTIVE,
			"updated note"
		);

		UserDetailResponse response = userService.updateUser(authenticatedUser(UserRole.ADMIN), 2L, request);

		assertThat(response.userId()).isEqualTo(2L);
		assertThat(response.name()).isEqualTo("updated");
		assertThat(response.phone()).isEqualTo("010-9999-8888");
		assertThat(response.role()).isEqualTo("MANAGER");
		assertThat(response.note()).isEqualTo("updated note");
		then(userMapper).should().updateUser(2L, request);
	}

	@Test
	void updateUserReturnsUpdatedDetailWhenRequesterIsManagerAndTargetIsNotAdmin() {
		User targetUser = user(2L, UserRole.VIEWER);
		User updatedUser = user(2L, UserRole.OPERATOR);
		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser), Optional.of(updatedUser));
		UserUpdateRequest request = new UserUpdateRequest(null, null, UserRole.OPERATOR, null, null, null);

		UserDetailResponse response = userService.updateUser(authenticatedUser(UserRole.MANAGER), 2L, request);

		assertThat(response.role()).isEqualTo("OPERATOR");
		then(userMapper).should().updateUser(2L, request);
	}

	@Test
	void updateUserThrowsAuthenticationRequiredWhenUserIsMissing() {
		UserUpdateRequest request = new UserUpdateRequest("updated", null, null, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(null, 2L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED)
			);
	}

	@Test
	void updateUserThrowsValidationErrorWhenRequestHasNoUpdates() {
		UserUpdateRequest request = new UserUpdateRequest(" ", null, null, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(authenticatedUser(UserRole.ADMIN), 2L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR)
			);

		then(userMapper).should(never()).findById(any());
		then(userMapper).should(never()).updateUser(any(), any());
	}

	@Test
	void updateUserThrowsAccessDeniedWhenRequesterIsViewer() {
		UserUpdateRequest request = new UserUpdateRequest("updated", null, null, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(authenticatedUser(UserRole.VIEWER), 2L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);

		then(userMapper).should(never()).findById(any());
		then(userMapper).should(never()).updateUser(any(), any());
	}

	@Test
	void updateUserThrowsAccessDeniedWhenManagerUpdatesAdminUser() {
		given(userMapper.findById(2L)).willReturn(Optional.of(user(2L, UserRole.ADMIN)));
		UserUpdateRequest request = new UserUpdateRequest("updated", null, null, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(authenticatedUser(UserRole.MANAGER), 2L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);

		then(userMapper).should(never()).updateUser(any(), any());
	}

	@Test
	void updateUserThrowsAccessDeniedWhenManagerPromotesUserToAdmin() {
		given(userMapper.findById(2L)).willReturn(Optional.of(user(2L, UserRole.OPERATOR)));
		UserUpdateRequest request = new UserUpdateRequest(null, null, UserRole.ADMIN, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(authenticatedUser(UserRole.MANAGER), 2L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);

		then(userMapper).should(never()).updateUser(any(), any());
	}

	@Test
	void updateUserThrowsNotFoundWhenTargetUserDoesNotExist() {
		given(userMapper.findById(999L)).willReturn(Optional.empty());
		UserUpdateRequest request = new UserUpdateRequest("updated", null, null, null, null, null);

		assertThatThrownBy(() -> userService.updateUser(authenticatedUser(UserRole.ADMIN), 999L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
			);
	}

	@Test
	void getUsersReturnsPagedUsersWhenRequesterIsAdmin() {
		given(userMapper.countUsers(any(UserListRequest.class))).willReturn(21L);
		given(userMapper.findUsers(any(UserListRequest.class))).willReturn(List.of(user(2L, UserRole.OPERATOR)));

		UserListResponse response = userService.getUsers(
			authenticatedUser(UserRole.ADMIN),
			UserListRequest.of(1, 10, " operator ", UserRole.OPERATOR, UserStatus.ACTIVE, 1L)
		);

		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalCount()).isEqualTo(21L);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).userId()).isEqualTo(2L);
		assertThat(response.items().get(0).role()).isEqualTo("OPERATOR");

		ArgumentCaptor<UserListRequest> requestCaptor = ArgumentCaptor.forClass(UserListRequest.class);
		then(userMapper).should().countUsers(requestCaptor.capture());
		UserListRequest request = requestCaptor.getValue();
		assertThat(request.page()).isEqualTo(1);
		assertThat(request.size()).isEqualTo(10);
		assertThat(request.offset()).isEqualTo(10);
		assertThat(request.keyword()).isEqualTo("operator");
		assertThat(request.role()).isEqualTo(UserRole.OPERATOR);
		assertThat(request.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(request.plantId()).isEqualTo(1L);
	}

	@Test
	void getUsersReturnsPagedUsersWhenRequesterIsManager() {
		given(userMapper.countUsers(any(UserListRequest.class))).willReturn(0L);
		given(userMapper.findUsers(any(UserListRequest.class))).willReturn(List.of());

		UserListResponse response = userService.getUsers(
			authenticatedUser(UserRole.MANAGER),
			UserListRequest.of(null, null, null, null, null, null)
		);

		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalCount()).isZero();
		assertThat(response.totalPages()).isZero();
		assertThat(response.items()).isEmpty();
	}

	@Test
	void getUsersThrowsAccessDeniedWhenRequesterIsViewer() {
		assertThatThrownBy(() -> userService.getUsers(
			authenticatedUser(UserRole.VIEWER),
			UserListRequest.of(0, 20, null, null, null, null)
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCESS_DENIED)
			);
	}

	@Test
	void getUserDetailReturnsUserWhenRequesterIsAdmin() {
		User targetUser = user(2L, UserRole.OPERATOR);
		given(userMapper.findById(2L)).willReturn(Optional.of(targetUser));

		UserDetailResponse response = userService.getUserDetail(authenticatedUser(UserRole.ADMIN), 2L);

		assertThat(response.userId()).isEqualTo(targetUser.getId());
		assertThat(response.email()).isEqualTo(targetUser.getEmail());
		assertThat(response.name()).isEqualTo(targetUser.getName());
		assertThat(response.role()).isEqualTo(targetUser.getRole().name());
		assertThat(response.note()).isEqualTo(targetUser.getNote());
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
	void userResponsesDoNotExposePasswordHash() {
		assertThat(UserDetailResponse.class.getRecordComponents())
			.extracting("name")
			.doesNotContain("passwordHash");
		assertThat(UserListItemResponse.class.getRecordComponents())
			.extracting("name")
			.doesNotContain("passwordHash", "note");
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

	private User user(Long userId, UserRole role) {
		return User.builder()
			.id(userId)
			.email("user" + userId + "@example.com")
			.passwordHash("encoded-password")
			.name("user" + userId)
			.phone("010-0000-000" + userId)
			.role(role)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.note("광명 공장 담당자")
			.lastLoginAt(LocalDateTime.of(2026, 5, 13, 10, 20, 30))
			.createdAt(LocalDateTime.of(2026, 5, 13, 9, 0, 0))
			.updatedAt(LocalDateTime.of(2026, 5, 13, 10, 0, 0))
			.build();
	}
}
