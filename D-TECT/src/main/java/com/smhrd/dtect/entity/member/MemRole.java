package com.smhrd.dtect.entity.member;

public enum MemRole {
    USER, ADMIN, EXPERT;

    public String getRoleName() {
        return "ROLE_" + this.name();
    }
}
