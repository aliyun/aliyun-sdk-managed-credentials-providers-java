package com.aliyun.kms.secretsmanager.plugin.common;


import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.service.DefaultSecretsManagerPluginCredentialsProviderLoader;
import com.aliyun.kms.secretsmanager.plugin.common.service.SecretsManagerPluginCredentialsProviderLoader;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.io.IOException;

public class AliyunSDKSecretsManagerPluginsManager {

    private static AliyunSDKSecretsManagerPlugin pluginInstance;

    public AliyunSDKSecretsManagerPluginsManager() {
    }

    public static void init() throws CacheSecretException {
        init(new DefaultSecretsManagerPluginCredentialsProviderLoader());
    }

    public static void init(final SecretsManagerPluginCredentialsProviderLoader loader) throws CacheSecretException {
        if (pluginInstance == null) {
            synchronized (AliyunSDKSecretsManagerPluginsManager.class) {
                if (pluginInstance == null) {
                    pluginInstance = new AliyunSDKSecretsManagerPlugin(loader);
                    pluginInstance.init();
                }
            }
        } else {
            CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).infof("SecretsManagerPluginManager has initialized");
        }
    }

    public static SecretsManagerPluginCredentials getAccessKey(String secretName) throws CacheSecretException {
        AliyunSDKSecretsManagerPlugin client = getSecretsManagerPlugin();
        return client.getAccessKey(secretName);
    }

    public static String getAccessKeyId(String secretName) throws CacheSecretException {
        return getAccessKey(secretName).getAccessKeyId();
    }

    public static String getAccessKeySecret(String secretName) throws CacheSecretException {
        return getAccessKey(secretName).getAccessKeySecret();
    }

    public static void refreshSecretInfo(String secretName) {
        AliyunSDKSecretsManagerPlugin client = getSecretsManagerPlugin();
        client.refreshSecretInfo(secretName);
    }

    public static AliyunSDKSecretsManagerPlugin getSecretsManagerPlugin() {
        if (pluginInstance == null) {
            throw new RuntimeException("Not initialize secrets manager plugin");
        }
        return pluginInstance;
    }

    public static void shutdown() throws IOException {
        if (pluginInstance == null) {
            throw new RuntimeException("Not initialize secrets manager plugin");
        }
        pluginInstance.shutdown();
    }

    protected static void registerClientInstance(AliyunSDKSecretsManagerPlugin pluginInstance) {
        if (AliyunSDKSecretsManagerPluginsManager.pluginInstance == null) {
            synchronized (AliyunSDKSecretsManagerPluginsManager.class) {
                if (AliyunSDKSecretsManagerPluginsManager.pluginInstance == null) {
                    AliyunSDKSecretsManagerPluginsManager.pluginInstance = pluginInstance;
                }
            }
        }
    }


}