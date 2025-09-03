package com.smhrd.dtect.controller;


public final class UsernameParam {
    private UsernameParam() {}

    public static String resolveUsername(String sid,
                                         String usernameParam,
                                         String userIdParam,  // n8n에서 userId에 username을 넣어줄 수 있으니 보조로 받음
                                         String securityUsername,
                                         java.util.function.Function<String,String> usernameBySid) {

        String u = (sid != null && !sid.isBlank()) ? usernameBySid.apply(sid) : null;
        if (isNotBlank(u)) return u;

        if (isNotBlank(usernameParam)) return usernameParam;

        if (isNotBlank(userIdParam)) return userIdParam;

        if (isNotBlank(securityUsername)) return securityUsername;

        return null;
    }

    private static boolean isNotBlank(String s){ return s != null && !s.isBlank(); }
}
