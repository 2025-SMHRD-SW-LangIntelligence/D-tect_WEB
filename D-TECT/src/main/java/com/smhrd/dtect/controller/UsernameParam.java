package com.smhrd.dtect.controller;


public final class UsernameParam {
    private UsernameParam() {}

    /** 우선순위: sid→세션→username 파라미터→보안컨텍스트 */
    public static String resolveUsername(String sid,
                                         String usernameParam,
                                         String userIdParam,  // n8n에서 userId에 username을 넣어줄 수 있으니 보조로 받음
                                         String securityUsername,
                                         java.util.function.Function<String,String> usernameBySid) {
        // sid → username
        String u = (sid != null && !sid.isBlank()) ? usernameBySid.apply(sid) : null;
        if (isNotBlank(u)) return u;

        // username 파라미터
        if (isNotBlank(usernameParam)) return usernameParam;

        // userId 파라미터(=username을 담아보낸 경우)
        if (isNotBlank(userIdParam)) return userIdParam;

        // 보안 컨텍스트
        if (isNotBlank(securityUsername)) return securityUsername;

        return null;
    }

    private static boolean isNotBlank(String s){ return s != null && !s.isBlank(); }
}
