package com.cxd.springframework;

public class StringUtil {
    /**
     * 检测目标方法是否是一个空对象或空字符串
     * @param str
     * @return
     */
    public static boolean isNotBlack(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
