package com.aliyun.kms.secretsmanager.plugin.sdkcore;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

public class ProxyAcsClient extends ProxyDefaultSdkCoreClient {

    public ProxyAcsClient(String secretName) throws ClientException {
        this(secretName, (AKExpireHandler<ClientException>) null);
    }

    public ProxyAcsClient(String secretName, AKExpireHandler<ClientException> handler) throws ClientException {
        super(secretName, handler);
        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
        super.setSecretsManagerPlugin(secretsManagerPlugin);
        initProxyAcsClient(DefaultProfile.getProfile(), secretName, handler);
    }

    public ProxyAcsClient(String regionId, String secretName) throws ClientException {
        this(regionId, secretName, null);
    }

    public ProxyAcsClient(String regionId, String secretName, AKExpireHandler<ClientException> handler) throws ClientException {
        super(regionId, secretName, handler);
        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
        super.setSecretsManagerPlugin(secretsManagerPlugin);
        initProxyAcsClient(regionId, secretName, handler);
    }

    public ProxyAcsClient(IClientProfile profile, String secretName) {
        this(profile, secretName, null);
    }

    public ProxyAcsClient(IClientProfile profile, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile, secretName, handler);
        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
        super.setSecretsManagerPlugin(secretsManagerPlugin);
        initProxyAcsClient(profile, secretName, handler);
    }

    public ProxyAcsClient(IClientProfile profile, AlibabaCloudCredentials credentials, String secretName) {
        this(profile, credentials, secretName, null);
    }

    public ProxyAcsClient(IClientProfile profile, AlibabaCloudCredentials credentials, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile, credentials, secretName, handler);
        throw new UnsupportedOperationException("Not support such constructors");
    }

    public ProxyAcsClient(IClientProfile profile, AlibabaCloudCredentialsProvider credentialsProvider, String secretName) {
        this(profile, credentialsProvider, secretName, null);
    }

    public ProxyAcsClient(IClientProfile profile, AlibabaCloudCredentialsProvider credentialsProvider, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile, credentialsProvider, secretName, handler);
        throw new UnsupportedOperationException("Not support such constructors");
    }

    protected void initProxyAcsClient(String regionId, String secretName, AKExpireHandler<ClientException> handler) {
        try {
            SecretsManagerSdkCorePluginManager.getSdkCoreClient(regionId, secretName, handler, this);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initProxyAcsClient(IClientProfile profile, String secretName, AKExpireHandler<ClientException> handler) {
        try {
            SecretsManagerSdkCorePluginManager.getSdkCoreClient((DefaultProfile) profile, secretName, handler, this);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown(this);
    }
}
