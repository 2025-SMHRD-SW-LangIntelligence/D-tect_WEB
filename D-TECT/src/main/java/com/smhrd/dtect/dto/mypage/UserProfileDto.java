package com.smhrd.dtect.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileDto {
    private Long userId;
    private String name;
    private String email;
    private String address;
}
