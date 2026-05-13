package com.smartfactory.scada.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 에러 응답")
public record ErrorResponse(
	@Schema(description = "프론트에서 분기 처리할 에러 코드", example = "VALIDATION_ERROR")
	String code,

	@Schema(description = "사용자 또는 개발자가 읽을 수 있는 에러 메시지", example = "요청 값이 올바르지 않습니다.")
	String message
) {

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
	}
}
