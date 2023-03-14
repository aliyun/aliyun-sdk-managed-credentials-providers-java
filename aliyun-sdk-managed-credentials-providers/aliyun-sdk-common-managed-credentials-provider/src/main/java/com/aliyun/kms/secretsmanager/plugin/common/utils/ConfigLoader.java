package com.aliyun.kms.secretsmanager.plugin.common.utils;

public class ConfigLoader {

    private static String configName;

    private ConfigLoader() {
    }

    public static String getConfigName() {
        return configName;
    }

    public static void setConfigName(String configName) {
        ConfigLoader.configName = configName;
    }
}
