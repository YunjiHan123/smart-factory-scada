package com.smartfactory.scada.auth.exception;

import org.springframework.http.HttpStatus;

import com.smartfactory.scada.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
