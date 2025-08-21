// src/main/java/com/smhrd/dtect/SignupRequestDTO.java
package com.smhrd.dtect;

import lombok.Data;

@Data
public class SignupRequestDTO {
    // 공통
    private String username;
    private String password;     // !!! step1에서 sessionStorage에 넣고, step2 전송 시 반드시 포함 !!!
    private String name;
    private String phone;
    private String email;
    private String address;      // 도로명/지번
    private String addrDetail;   // 상세
    private String termsAgree;   // "Y" / "N"
    private boolean expert;      // 전문가 여부

    // 전문가 전용
    private String officeName;     // (필요하다면)
    private String officeAddress;  // step1에서 officeAddress를 받았다면 여기에
}
