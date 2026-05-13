package com.smartfactory.scada.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@Schema(description = "회원가입에 사용할 이메일", example = "new-user@example.com")
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이어야 합니다.")
	String email,

	@Schema(description = "8자 이상 비밀번호", example = "password1234!")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
	String password,

	@Schema(description = "사용자 이름", example = "조혜원")
	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@Schema(description = "사용자 전화번호", example = "010-0000-0000")
	String phone,

	@Schema(description = "사용자가 소속된 사업장 ID", example = "1")
	Long plantId
) {
}
