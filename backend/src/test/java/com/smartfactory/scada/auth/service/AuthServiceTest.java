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
import com.smartfactory.scada.auth.exception.AuthErrorCode;
import com.smartfactory.scada.auth.jwt.JwtTokenProvider;
import com.smartfactory.scada.common.exception.BusinessException;
import com.smartfactory.scada.user.domain.User;
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
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester");
		given(userMapper.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encoded-password");

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		then(userMapper).should().insert(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo(request.email());
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(savedUser.getNickname()).isEqualTo(request.nickname());
		assertThat(response.email()).isEqualTo(request.email());
		assertThat(response.nickname()).isEqualTo(request.nickname());
	}

	@Test
	void signupThrowsExceptionWhenEmailAlreadyExists() {
		SignupRequest request = new SignupRequest("exists@example.com", "password1234!", "tester");
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
		SignupRequest request = new SignupRequest("new-user@example.com", "password1234!", "tester");
		given(userMapper.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encoded-password");

		SignupResponse response = authService.signup(request);

		assertThat(response.getClass().getRecordComponents())
			.extracting("name")
			.containsExactly("userId", "email", "nickname");
	}

	@Test
	void loginReturnsTokensWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest("login@example.com", "password1234!");
		User user = User.builder()
			.id(1L)
			.email(request.email())
			.passwordHash("encoded-password")
			.nickname("tester")
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(true);
		given(jwtTokenProvider.createAccessToken(user.getId())).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(user.getId())).willReturn("refresh-token");

		LoginResponse response = authService.login(request);

		assertThat(response.userId()).isEqualTo(user.getId());
		assertThat(response.email()).isEqualTo(user.getEmail());
		assertThat(response.nickname()).isEqualTo(user.getNickname());
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		then(refreshTokenService).should().save(user.getId(), "refresh-token");
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
			.nickname("tester")
			.build();
		given(userMapper.findByEmail(request.email())).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.password(), user.getPasswordHash())).willReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_LOGIN)
			);

		then(refreshTokenService).shouldHaveNoInteractions();
	}
}
