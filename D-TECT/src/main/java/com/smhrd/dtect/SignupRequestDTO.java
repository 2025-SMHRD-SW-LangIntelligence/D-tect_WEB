package com.smhrd.dtect;

import com.smhrd.dtect.entity.FieldName;
import lombok.Data;

import java.util.List;

@Data
public class SignupRequestDTO {
    // 공통
    private String username;
    private String password;   // step1에서 세션으로 왔든, 폼에서 입력했든 최종 포함
    private String name;
    private String phone;
    private String email;
    private String address;
    private String addrDetail;
    private String termsAgree; // "Y"/"N"
    private boolean expert;    // 전문가 여부

    // 전문가 전용
    private String officeName;
    private String officeAddress;
    private List<FieldName> fields;
}
