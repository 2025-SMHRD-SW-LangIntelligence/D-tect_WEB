	// com/smhrd/dtect/dto/WithdrawRequest.java
package com.smhrd.dtect.dto.member;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WithdrawRequest {
    private String currentPassword;   // 필수: 비밀번호 확인
    private Boolean eraseData;        // 선택: 개인정보 익명화/정리 여부(선택)
    private String reason;            // 선택: 사유(저장할지 말지는 정책에 맞게)
}
