package com.smhrd.dtect.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMeRequest {
    private String name;
    private String email;

    // USER
    private String address;

    // EXPERT
    private String officeName;
    private String officeAddress;
    private List<String> specialtyCodes;

    // 비밀번호
    private String currentPassword;
    private boolean changePassword;
    private String newPassword;
    private String newPasswordConfirm;
}
