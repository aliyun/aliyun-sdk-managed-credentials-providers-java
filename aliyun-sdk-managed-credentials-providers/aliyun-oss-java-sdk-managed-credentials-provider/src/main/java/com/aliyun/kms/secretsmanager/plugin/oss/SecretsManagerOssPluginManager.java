package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSS;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.io.IOException;

public class SecretsManagerOssPluginManager {

    private static AKExpireHandler<ClientException> akExpireHandler;
    private static SecretsManagerOssPlugin client;

    public static OSS getOssClient(String endpoint, ClientConfiguration config, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getOssClient(endpoint, config, secretName);
    }

    public static OSS getOssClient(String endpoint, ClientConfiguration config, String secretName, AKExpireHandler akExpireHandler) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getOssClient(endpoint, config, secretName, akExpireHandler);
    }

    public static OSS getOssClient(String endpoint, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getOssClient(endpoint, secretName);
    }

    public static OSS getOssClient(String endpoint, String secretName, AKExpireHandler akExpireHandler) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getOssClient(endpoint, secretName, akExpireHandler);
    }

    public static void closeOssClient(OSS ossClient, String secretName) throws IOException {
        initSecretsManagerPlugin();
        client.closeOssClient(ossClient, secretName);
    }

    public static void setAKExpireHandler(AKExpireHandler<ClientException> akExpireHandler) {
        SecretsManagerOssPluginManager.akExpireHandler = akExpireHandler;
    }

    public static void destroy() throws IOException {
        if (client != null) {
            client.destroy();
        }
    }

    private static void initSecretsManagerPlugin() {
        if (client == null) {
            synchronized (SecretsManagerOssPluginManager.class) {
                if (client == null) {
                    try {
                        AliyunSDKSecretsManagerPluginsManager.init();
                        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
                        client = new SecretsManagerOssPlugin(secretsManagerPlugin);
                        if (akExpireHandler != null) {
                            client.setAkExpireHandler(akExpireHandler);
                        }
                    } catch (CacheSecretException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
