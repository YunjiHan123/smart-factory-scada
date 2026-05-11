package com.smartfactory.scada.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "\uC774\uBA54\uC77C\uC740 \uD544\uC218\uC785\uB2C8\uB2E4.")
	@Email(message = "\uC62C\uBC14\uB978 \uC774\uBA54\uC77C \uD615\uC2DD\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4.")
	String email,

	@NotBlank(message = "\uBE44\uBC00\uBC88\uD638\uB294 \uD544\uC218\uC785\uB2C8\uB2E4.")
	String password
) {
}
