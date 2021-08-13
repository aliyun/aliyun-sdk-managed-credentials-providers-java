package com.aliyun.kms.secretsmanager.plugin.sdkcore;

import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.profile.DefaultProfile;

import java.io.IOException;

public class SecretsManagerSdkCorePluginManager {

    private static AKExpireHandler<ClientException> akExpireHandler;
    private static SecretsManagerSdkCorePlugin client;

    public static IAcsClient getSdkCoreClient(String regionId, String secretName) throws CacheSecretException {
        return getSdkCoreClient(regionId, secretName, null);
    }

    public static IAcsClient getSdkCoreClient(String regionId, String secretName, AKExpireHandler<ClientException> akExpireHandle) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getSdkCoreClient(regionId, secretName, akExpireHandle);
    }

    protected static IAcsClient getSdkCoreClient(String regionId, String secretName, AKExpireHandler<ClientException> akExpireHandle, ProxyDefaultSdkCoreClient acsClient) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getSdkCoreClient(regionId, secretName, akExpireHandle, acsClient);
    }

    protected static IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName, AKExpireHandler<ClientException> akExpireHandle, ProxyDefaultSdkCoreClient acsClient) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getSdkCoreClient(profile, secretName, akExpireHandle, acsClient);
    }

    public static IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getSdkCoreClient(profile, secretName);
    }

    public static IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName, AKExpireHandler<ClientException> akExpireHandle) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.getSdkCoreClient(profile, secretName, akExpireHandle);
    }

    public static void closeSdkCoreClient(IAcsClient acsClient, String secretName) throws IOException {
        initSecretsManagerPlugin();
        client.closeSdkCoreClient(acsClient, secretName);
    }

    public static void setAKExpireHandler(AKExpireHandler<ClientException> akExpireHandler) {
        SecretsManagerSdkCorePluginManager.akExpireHandler = akExpireHandler;
    }

    public static void destroy() throws IOException {
        if (client != null) {
            client.destroy();
        }
    }

    private static void initSecretsManagerPlugin() {
        if (client == null) {
            synchronized (SecretsManagerSdkCorePluginManager.class) {
                if (client == null) {
                    try {
                        AliyunSDKSecretsManagerPluginsManager.init();
                        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
                        client = new SecretsManagerSdkCorePlugin(secretsManagerPlugin);
                        if (akExpireHandler != null) {
                            client.setAkExpireHandler(akExpireHandler);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
