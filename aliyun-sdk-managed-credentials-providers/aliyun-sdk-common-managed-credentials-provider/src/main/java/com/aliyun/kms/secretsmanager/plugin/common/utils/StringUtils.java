package com.aliyun.kms.secretsmanager.plugin.common.utils;

public class StringUtils {

    private StringUtils() {
        // do nothing
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
