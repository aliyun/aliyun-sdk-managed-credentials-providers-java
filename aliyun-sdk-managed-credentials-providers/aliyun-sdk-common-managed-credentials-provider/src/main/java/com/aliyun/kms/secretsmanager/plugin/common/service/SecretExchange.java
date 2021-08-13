package com.aliyun.kms.secretsmanager.plugin.common.service;

public interface SecretExchange {

    /**
     * exchange secret name by user secret name
     *
     * @param userSecretName
     * @return
     */
    default String exchangeSecretName(String userSecretName) {
        return userSecretName;
    }
}
