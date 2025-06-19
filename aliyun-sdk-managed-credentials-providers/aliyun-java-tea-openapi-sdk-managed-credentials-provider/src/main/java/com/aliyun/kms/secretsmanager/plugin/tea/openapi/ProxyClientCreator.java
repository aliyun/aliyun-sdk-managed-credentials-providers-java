package com.aliyun.kms.secretsmanager.plugin.tea.openapi;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.Config;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

public class ProxyClientCreator {
    public static <T extends Client> T createClient(String endpoint, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        return SecretsManagerTeaOpenApiPluginManager.getTeaOpenApiClient(endpoint, clientClass, secretName);
    }

    public static <T extends Client> T createClient(Config config, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        return SecretsManagerTeaOpenApiPluginManager.getTeaOpenApiClient(config, clientClass, secretName);
    }

    public static <T extends Client> T createClient(String endpoint, Class<? extends Client> clientClass, String secretName, AKExpireHandler<TeaException> akExpireHandler) throws CacheSecretException {
        return SecretsManagerTeaOpenApiPluginManager.getTeaOpenApiClient(endpoint, clientClass, secretName, akExpireHandler);
    }

    public static <T extends Client> T createClient(Config config, Class<? extends Client> clientClass, String secretName, AKExpireHandler<TeaException> akExpireHandler) throws CacheSecretException {
        return SecretsManagerTeaOpenApiPluginManager.getTeaOpenApiClient(config, clientClass, secretName, akExpireHandler);
    }
}
