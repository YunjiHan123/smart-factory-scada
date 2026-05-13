package com.smartfactory.scada.user.exception;

import org.springframework.http.HttpStatus;

import com.smartfactory.scada.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "\uC0AC\uC6A9\uC790\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."),
	USER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "USER_ACCESS_DENIED", "\uC0AC\uC6A9\uC790 \uAD00\uB9AC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
