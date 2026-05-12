package com.smartfactory.scada.auth.exception;

import org.springframework.http.HttpStatus;

import com.smartfactory.scada.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "\uC774\uBBF8 \uAC00\uC785\uB41C \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4."),
	INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "\uC774\uBA54\uC77C \uB610\uB294 \uBE44\uBC00\uBC88\uD638\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."),
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "\uC778\uC99D\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "\uC720\uD6A8\uD558\uC9C0 \uC54A\uC740 \uD1A0\uD070\uC785\uB2C8\uB2E4."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "\uB9CC\uB8CC\uB41C \uD1A0\uD070\uC785\uB2C8\uB2E4."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "\uC800\uC7A5\uB41C \uB9AC\uD504\uB808\uC2DC \uD1A0\uD070\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."),
	REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "\uB9AC\uD504\uB808\uC2DC \uD1A0\uD070\uC774 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
