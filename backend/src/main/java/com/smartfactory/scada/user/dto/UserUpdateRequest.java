package com.smartfactory.scada.user.dto;

import org.springframework.util.StringUtils;

import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserUpdateRequest(
	@Schema(description = "변경할 사용자 이름. null, 누락, 공백이면 수정하지 않습니다.", example = "조혜원")
	String name,

	@Schema(description = "변경할 전화번호. null, 누락, 공백이면 수정하지 않습니다.", example = "010-1234-5678")
	String phone,

	@Schema(description = "변경할 사용자 권한. MANAGER는 ADMIN으로 변경할 수 없습니다.", example = "OPERATOR")
	UserRole role,

	@Schema(description = "변경할 사업장 ID. null 또는 누락이면 수정하지 않습니다.", example = "1")
	Long plantId,

	@Schema(description = "변경할 사용자 상태", example = "ACTIVE")
	UserStatus status,

	@Schema(description = "변경할 사용자 비고. null, 누락, 공백이면 수정하지 않습니다. v1에서는 null로 비우기를 지원하지 않습니다.", example = "광명 공장 담당자")
	String note
) {

	public boolean hasUpdates() {
		return StringUtils.hasText(name)
			|| StringUtils.hasText(phone)
			|| role != null
			|| plantId != null
			|| status != null
			|| StringUtils.hasText(note);
	}
}
