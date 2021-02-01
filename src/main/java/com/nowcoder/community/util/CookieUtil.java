package com.nowcoder.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * 从request获取用户登录凭证，就是获取cookie信息
 */
public class CookieUtil {

    public static String getCookie(HttpServletRequest request, String ticket) {

        if (request == null || ticket == null) {
            throw new IllegalArgumentException("参数不合法");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(ticket)) {//cookie的设置是key-value键值对
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
