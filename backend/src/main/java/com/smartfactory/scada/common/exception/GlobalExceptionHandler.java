package com.smartfactory.scada.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.from(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException() {
		return ResponseEntity
			.status(CommonErrorCode.VALIDATION_ERROR.getStatus())
			.body(ErrorResponse.from(CommonErrorCode.VALIDATION_ERROR));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException() {
		return ResponseEntity
			.status(CommonErrorCode.VALIDATION_ERROR.getStatus())
			.body(ErrorResponse.from(CommonErrorCode.VALIDATION_ERROR));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException() {
		return ResponseEntity
			.status(CommonErrorCode.VALIDATION_ERROR.getStatus())
			.body(ErrorResponse.from(CommonErrorCode.VALIDATION_ERROR));
	}
}
