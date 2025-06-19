package com.aliyun.kms.secretsmanager.plugin.tea.openapi;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.Config;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;


import java.io.IOException;

public class SecretsManagerTeaOpenApiPluginManager {

    private static volatile SecretsManagerTeaOpenApiPlugin teaOpenApiPlugin;

    public static <T extends Client> T getTeaOpenApiClient(String endpoint, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return teaOpenApiPlugin.getTeaOpenApiClient(endpoint, clientClass, secretName);
    }

    public static <T extends Client> T getTeaOpenApiClient(Config config, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return teaOpenApiPlugin.getTeaOpenApiClient(config, clientClass, secretName);
    }

    public static <T extends Client> T getTeaOpenApiClient(String endpoint, Class<? extends Client> clientClass, String secretName, AKExpireHandler<TeaException> akExpireHandler) throws CacheSecretException {
        initSecretsManagerPlugin();
        if (akExpireHandler != null) {
            teaOpenApiPlugin.setAkExpireHandler(akExpireHandler);
        }
        return teaOpenApiPlugin.getTeaOpenApiClient(endpoint, clientClass, secretName);
    }

    public static <T extends Client> T getTeaOpenApiClient(Config config, Class<? extends Client> clientClass, String secretName, AKExpireHandler<TeaException> akExpireHandler) throws CacheSecretException {
        initSecretsManagerPlugin();
        if (akExpireHandler != null) {
            teaOpenApiPlugin.setAkExpireHandler(akExpireHandler);
        }
        return teaOpenApiPlugin.getTeaOpenApiClient(config, clientClass, secretName);
    }

    public static <T extends Client> void closeOpenApiClient(T openApiClient, String secretName) throws IOException {
        initSecretsManagerPlugin();
        teaOpenApiPlugin.closeOpenApiClient(openApiClient, secretName);
    }

    public static void destroy() throws IOException {
        if (teaOpenApiPlugin != null) {
            teaOpenApiPlugin.destroy();
        }
    }

    private static void initSecretsManagerPlugin() {
        if (teaOpenApiPlugin == null) {
            synchronized (SecretsManagerTeaOpenApiPluginManager.class) {
                if (teaOpenApiPlugin == null) {
                    try {
                        AliyunSDKSecretsManagerPluginsManager.init();
                        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
                        teaOpenApiPlugin = new SecretsManagerTeaOpenApiPlugin(secretsManagerPlugin);
                    } catch (CacheSecretException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
