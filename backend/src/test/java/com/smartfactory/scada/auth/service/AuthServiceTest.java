package com.smartfactory.scada.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AuthService authService;

	@Test
	void signupSavesUserWithEncodedPassword() {
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester", "010-0000-0000", 1L);
		given(userMapper.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encoded-password");

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		then(userMapper).should().insert(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo(request.email());
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(savedUser.getName()).isEqualTo(request.name());
		assertThat(savedUser.getPhone()).isEqualTo(request.phone());
		assertThat(savedUser.getPlantId()).isEqualTo(request.plantId());
		assertThat(savedUser.getRole()).isEqualTo(UserRole.VIEWER);
		assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(response.email()).isEqualTo(request.email());
		assertThat(response.name()).isEqualTo(request.name());
		assertThat(response.phone()).isEqualTo(request.phone());
		assertThat(response.role()).isEqualTo(UserRole.VIEWER.name());
		assertThat(response.plantId()).isEqualTo(request.plantId());
		assertThat(response.status()).isEqualTo(UserStatus.ACTIVE.name());
	}

	@Test
	void signupThrowsExceptionWhenEmailAlreadyExists() {
		SignupRequest request = new SignupRequest("exists@example.com", "password1234!", "tester", "010-0000-0000", 1L);
		given(userMapper.existsByEmail(request.email())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS)
			);

		then(passwordEncoder).shouldHaveNoInteractions();
		then(userMapper).should().existsByEmail(request.email());
		then(userMapper).shouldHaveNoMoreInteractions();
	}

	@Test
	void signupResponseDoesNotExposePasswordHash() {
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester", "010-0000-0000", 1L);
		given(userMapper.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encoded-password");

		SignupResponse response = authService.signup(request);

		assertThat(response.getClass().getRecordComponents())
			.extracting("name")
			.containsExactly("userId", "email", "name", "phone", "role", "plantId", "status");
	}

	@Test
	void loginReturnsTokensWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest("login@example.com", "password1234!");
		User user = User.builder()
			.id(1L)
			.email(request.email())
			.passwordHash("encoded-password")
			.name("tester")
			.phone("010-0000-0000")
			.role(UserRole.VIEWER)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(true);
		given(jwtTokenProvider.createAccessToken(user.getId())).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(user.getId())).willReturn("refresh-token");

		LoginResponse response = authService.login(request);

		assertThat(response.userId()).isEqualTo(user.getId());
		assertThat(response.email()).isEqualTo(user.getEmail());
		assertThat(response.name()).isEqualTo(user.getName());
		assertThat(response.role()).isEqualTo(UserRole.VIEWER.name());
		assertThat(response.plantId()).isEqualTo(user.getPlantId());
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		then(refreshTokenService).should().save(user.getId(), "refresh-token");
		then(userMapper).should().updateLastLoginAt(user.getId());
	}

	@Test
	void loginThrowsInvalidLoginWhenEmailDoesNotExist() {
		LoginRequest request = new LoginRequest("missing@example.com", "password1234!");
		given(userMapper.findByEmail(request.email())).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_LOGIN)
			);

		then(passwordEncoder).shouldHaveNoInteractions();
		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void loginThrowsInvalidLoginWhenPasswordDoesNotMatch() {
		LoginRequest request = new LoginRequest("login@example.com", "wrong-password");
		User user = User.builder()
			.id(1L)
			.email(request.email())
			.passwordHash("encoded-password")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.ACTIVE)
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_LOGIN)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void loginThrowsUserInactiveWhenUserIsInactive() {
		LoginRequest request = new LoginRequest("inactive@example.com", "password1234!");
		User user = User.builder()
			.id(1L)
			.email(request.email())
			.passwordHash("encoded-password")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.INACTIVE)
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(true);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_INACTIVE)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void loginThrowsUserLockedWhenUserIsLocked() {
		LoginRequest request = new LoginRequest("locked@example.com", "password1234!");
		User user = User.builder()
			.id(1L)
			.email(request.email())
			.passwordHash("encoded-password")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.LOCKED)
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(true);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_LOCKED)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void refreshReturnsNewTokenPairWhenTokensAreValid() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(activeUser()));
		given(refreshTokenService.exists(1L)).willReturn(true);
		given(refreshTokenService.matches(1L, "refresh-token")).willReturn(true);
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-refresh-token");

		TokenPair tokenPair = authService.refresh("access-token", "refresh-token");

		assertThat(tokenPair.accessToken()).isEqualTo("new-access-token");
		assertThat(tokenPair.refreshToken()).isEqualTo("new-refresh-token");
		then(refreshTokenService).should().save(1L, "new-refresh-token");
	}

	@Test
	void refreshThrowsInvalidTokenWhenAccessTokenIsInvalid() {
		given(jwtTokenProvider.getUserIdAllowExpired("invalid-access-token", TokenType.ACCESS))
			.willThrow(new IllegalArgumentException("invalid"));

		assertThatThrownBy(() -> authService.refresh("invalid-access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
			);
	}

	@Test
	void refreshThrowsExpiredTokenWhenRefreshTokenIsExpired() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("expired-refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.EXPIRED);

		assertThatThrownBy(() -> authService.refresh("access-token", "expired-refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EXPIRED_TOKEN)
			);
	}

	@Test
	void refreshThrowsInvalidTokenWhenTokenUsersAreDifferent() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(2L);

		assertThatThrownBy(() -> authService.refresh("access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
			);
	}

	@Test
	void refreshThrowsNotFoundWhenSavedRefreshTokenDoesNotExist() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(activeUser()));
		given(refreshTokenService.exists(1L)).willReturn(false);

		assertThatThrownBy(() -> authService.refresh("access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND)
			);
	}

	@Test
	void refreshThrowsMismatchWhenSavedRefreshTokenDoesNotMatch() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(activeUser()));
		given(refreshTokenService.exists(1L)).willReturn(true);
		given(refreshTokenService.matches(1L, "refresh-token")).willReturn(false);

		assertThatThrownBy(() -> authService.refresh("access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH)
			);
	}

	@Test
	void refreshThrowsUserLockedWhenUserIsLocked() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("locked@example.com")
			.passwordHash("encoded-password")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.LOCKED)
			.build()));

		assertThatThrownBy(() -> authService.refresh("access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_LOCKED)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void refreshThrowsUserInactiveWhenUserIsInactive() {
		given(jwtTokenProvider.getUserIdAllowExpired("access-token", TokenType.ACCESS)).willReturn(1L);
		given(jwtTokenProvider.validateToken("refresh-token", TokenType.REFRESH)).willReturn(TokenStatus.VALID);
		given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
		given(userMapper.findById(1L)).willReturn(Optional.of(User.builder()
			.id(1L)
			.email("inactive@example.com")
			.passwordHash("encoded-password")
			.name("tester")
			.role(UserRole.VIEWER)
			.status(UserStatus.INACTIVE)
			.build()));

		assertThatThrownBy(() -> authService.refresh("access-token", "refresh-token"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_INACTIVE)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}

	@Test
	void logoutDeletesSavedRefreshToken() {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
			1L,
			"login@example.com",
			"tester",
			"010-0000-0000",
			UserRole.VIEWER.name(),
			1L,
			UserStatus.ACTIVE.name()
		);

		authService.logout(authenticatedUser);

		then(refreshTokenService).should().delete(1L);
	}

	private User activeUser() {
		return User.builder()
			.id(1L)
			.email("login@example.com")
			.passwordHash("encoded-password")
			.name("tester")
			.phone("010-0000-0000")
			.role(UserRole.VIEWER)
			.plantId(1L)
			.status(UserStatus.ACTIVE)
			.build();
	}
}
