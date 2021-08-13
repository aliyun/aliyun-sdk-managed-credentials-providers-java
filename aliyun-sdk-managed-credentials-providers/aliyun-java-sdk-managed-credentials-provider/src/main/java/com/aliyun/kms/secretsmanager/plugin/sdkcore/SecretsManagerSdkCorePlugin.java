package com.aliyun.kms.secretsmanager.plugin.sdkcore;

import com.aliyun.kms.secretsmanager.plugin.common.CloudClientBuilder;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyuncs.*;
import com.aliyuncs.auth.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class SecretsManagerSdkCorePlugin {

    private static Set<Class<? extends SecretsManagerPluginCredentialUpdater>> secretsManagerCredentialUpdaterSet = new HashSet();
    private final static String CREDENTIALS_PROVIDER_FIELD_NAME = "credentialsProvider";
    private AKExpireHandler<ClientException> akExpireHandler;
    private AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    static {
        secretsManagerCredentialUpdaterSet.add(SdkCorePluginCredentialUpdater.class);
    }

    /**
     * use for spring bean
     */
    public SecretsManagerSdkCorePlugin() {
    }

    public SecretsManagerSdkCorePlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    /**
     * use for spring bean
     *
     * @param secretsManagerPlugin
     */
    public void setSecretsManagerPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    /**
     * use for spring bean
     *
     * @param akExpireHandler
     */
    public void setAkExpireHandler(AKExpireHandler<ClientException> akExpireHandler) {
        this.akExpireHandler = akExpireHandler;
    }


    public IAcsClient getSdkCoreClient(String regionId, String secretName) throws CacheSecretException {
        return getSdkCoreClient(DefaultProfile.getProfile(regionId), secretName);
    }

    public IAcsClient getSdkCoreClient(String regionId, String secretName, AKExpireHandler<ClientException> akExpireHandler) throws CacheSecretException {
        return getSdkCoreClient(DefaultProfile.getProfile(regionId), secretName, akExpireHandler);
    }

    public IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName) throws CacheSecretException {
        return getSdkCoreClient(profile, secretName, null);
    }

    public IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName, AKExpireHandler<ClientException> akExpireHandler) throws CacheSecretException {
        return getSdkCoreClient(profile, secretName, akExpireHandler, null);
    }

    protected IAcsClient getSdkCoreClient(String regionId, String secretName, AKExpireHandler<ClientException> akExpireHandler, ProxyDefaultSdkCoreClient acsClient) throws CacheSecretException {
        return getSdkCoreClient(DefaultProfile.getProfile(regionId), secretName, akExpireHandler, acsClient);
    }

    protected IAcsClient getSdkCoreClient(DefaultProfile profile, String secretName, AKExpireHandler<ClientException> akExpireHandler, ProxyDefaultSdkCoreClient acsClient) throws CacheSecretException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        SecretInfo secretInfo = secretsManagerPlugin.getSecretInfo(realSecretName);
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        SdkCorePluginCredentialProvider provider = new SdkCorePluginCredentialProvider(new BasicCredentials(credentials.getAccessKeyId(), credentials.getAccessKeySecret()));
        if (akExpireHandler == null) {
            akExpireHandler = this.akExpireHandler == null ? new SdkCorePluginAKExpireHandler() : this.akExpireHandler;
        }
        IAcsClient client = acsClient;
        if (client == null) {
            client = new SdkCoreClientBuilder(provider, profile, secretName, akExpireHandler).build();
        } else {
            Class<?> defaultAcsClientClazz = client.getClass().getSuperclass().getSuperclass();
            try {
                Field credentialsProvider = defaultAcsClientClazz.getDeclaredField(CREDENTIALS_PROVIDER_FIELD_NAME);
                credentialsProvider.setAccessible(true);
                credentialsProvider.set(client, provider);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        SecretsManagerPluginCredentialUpdater pluginCredentialUpdater = new SdkCorePluginCredentialUpdater(client, provider);
        secretsManagerPlugin.registerSecretsManagerPluginUpdater(secretInfo.getSecretName(), pluginCredentialUpdater);
        return client;
    }

    public void closeSdkCoreClient(IAcsClient client, String secretName) throws IOException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(realSecretName, client);
    }

    public void destroy() throws IOException {
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(secretsManagerCredentialUpdaterSet);
    }

    class SdkCoreClientBuilder implements CloudClientBuilder<IAcsClient> {
        private final String secretName;
        private final AKExpireHandler<ClientException> handler;
        private final SdkCorePluginCredentialProvider provider;

        private DefaultProfile defaultProfile;

        public SdkCoreClientBuilder(SdkCorePluginCredentialProvider provider, DefaultProfile defaultProfile, String secretName, AKExpireHandler<ClientException> handler) {
            this.secretName = secretName;
            this.handler = handler;
            this.provider = provider;
            this.defaultProfile = defaultProfile;
        }

        @Override
        public IAcsClient build() {
            if (provider == null) {
                throw new IllegalArgumentException("Provider cannot be null.");
            }
            if (defaultProfile == null) {
                defaultProfile = DefaultProfile.getProfile();
            }
            SecretsManagerPluginProfile profile = new SecretsManagerPluginProfile(defaultProfile, provider);
            ProxyDefaultSdkCoreClient client = new ProxyDefaultSdkCoreClient(profile, provider, secretName, handler);
            client.setSecretsManagerPlugin(secretsManagerPlugin);
            return client;
        }

        private class SecretsManagerPluginProfile implements IClientProfile {

            private final DefaultProfile defaultProfile;
            private final SdkCorePluginCredentialProvider provider;

            private SecretsManagerPluginProfile(DefaultProfile defaultProfile, SdkCorePluginCredentialProvider provider) {
                this.defaultProfile = defaultProfile;
                this.provider = provider;
            }

            @Override
            public ISigner getSigner() {
                return defaultProfile.getSigner();
            }

            @Override
            public String getRegionId() {
                return defaultProfile.getRegionId();
            }

            @Override
            public FormatType getFormat() {
                return defaultProfile.getFormat();
            }

            @Override
            public Credential getCredential() {
                return defaultProfile.getCredential();
            }

            @Override
            public void setCredentialsProvider(AlibabaCloudCredentialsProvider alibabaCloudCredentialsProvider) {
                defaultProfile.setCredentialsProvider(this.provider);
            }

            @Override
            public String getCertPath() {
                return defaultProfile.getCertPath();
            }

            @Override
            public void setCertPath(String s) {
                defaultProfile.setCertPath(s);
            }

            @Override
            public HttpClientConfig getHttpClientConfig() {
                return defaultProfile.getHttpClientConfig();
            }

            @Override
            public void setHttpClientConfig(HttpClientConfig httpClientConfig) {
                defaultProfile.setHttpClientConfig(httpClientConfig);
            }

            @Override
            public void enableUsingInternalLocationService() {
                defaultProfile.enableUsingInternalLocationService();
            }

            @Override
            public boolean isUsingInternalLocationService() {
                return defaultProfile.isUsingInternalLocationService();
            }

            @Override
            public boolean isUsingVpcEndpoint() {
                return defaultProfile.isUsingVpcEndpoint();
            }

            @Override
            public void enableUsingVpcEndpoint() {
                defaultProfile.enableUsingVpcEndpoint();
            }

            @Override
            public void setUsingInternalLocationService() {
                defaultProfile.setUsingInternalLocationService();
            }

            @Override
            public Logger getLogger() {
                return defaultProfile.getLogger();
            }

            @Override
            public void setLogger(Logger logger) {
                defaultProfile.setLogger(logger);
            }

            @Override
            public String getLogFormat() {
                return defaultProfile.getLogFormat();
            }

            @Override
            public void setLogFormat(String s) {
                defaultProfile.setLogFormat(s);
            }

            @Override
            public boolean isCloseTrace() {
                return defaultProfile.isCloseTrace();
            }

            @Override
            public void setCloseTrace(boolean b) {
                defaultProfile.setCloseTrace(b);
            }
        }
    }

}

class ProxyDefaultSdkCoreClient extends DefaultAcsClient {

    private final String secretName;
    private final AKExpireHandler<ClientException> handler;
    private boolean isClosing;
    private AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    public ProxyDefaultSdkCoreClient(String secretName, AKExpireHandler<ClientException> handler) throws ClientException {
        super();
        this.secretName = secretName;
        if (handler == null) {
            handler = new SdkCorePluginAKExpireHandler();
        }
        this.handler = handler;
    }

    public ProxyDefaultSdkCoreClient(String regionId, String secretName, AKExpireHandler<ClientException> handler) throws ClientException {
        super(regionId);
        this.secretName = secretName;
        if (handler == null) {
            handler = new SdkCorePluginAKExpireHandler();
        }
        this.handler = handler;
    }

    public ProxyDefaultSdkCoreClient(IClientProfile profile, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile);
        this.secretName = secretName;
        if (handler == null) {
            handler = new SdkCorePluginAKExpireHandler();
        }
        this.handler = handler;
    }

    public ProxyDefaultSdkCoreClient(IClientProfile profile, AlibabaCloudCredentials credentials, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile, credentials);
        this.secretName = secretName;
        this.handler = handler;
        throw new UnsupportedOperationException("Not support such constructors");
    }

    public ProxyDefaultSdkCoreClient(IClientProfile profile, AlibabaCloudCredentialsProvider credentialsProvider, String secretName, AKExpireHandler<ClientException> handler) {
        super(profile, credentialsProvider);
        this.secretName = secretName;
        if (handler == null) {
            handler = new SdkCorePluginAKExpireHandler();
        }
        this.handler = handler;
    }

    public void setSecretsManagerPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    private boolean judgeTempAKExpire(ClientException e) {
        return this.handler.judgeAKExpire(e);
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request) throws ClientException, ServerException {
        try {
            return super.doAction(request);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts) throws ClientException, ServerException {
        try {
            return super.doAction(request, autoRetry, maxRetryCounts);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request, autoRetry, maxRetryCounts);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, IClientProfile profile) throws ClientException, ServerException {
        try {
            return super.doAction(request, profile);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request, profile);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, String regionId, Credential credential) throws ClientException, ServerException {
        try {
            return super.doAction(request, regionId, credential);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request, regionId, credential);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request) throws ServerException, ClientException {
        try {
            return super.getAcsResponse(request);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getAcsResponse(request);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts) throws ServerException, ClientException {
        try {
            return super.getAcsResponse(request, autoRetry, maxRetryCounts);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getAcsResponse(request, autoRetry, maxRetryCounts);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, IClientProfile profile) throws ServerException, ClientException {
        try {
            return super.getAcsResponse(request, profile);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getAcsResponse(request, profile);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, String regionId, Credential credential) throws ServerException, ClientException {
        try {
            return super.getAcsResponse(request, regionId, credential);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getAcsResponse(request, regionId, credential);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, String regionId) throws ServerException, ClientException {
        try {
            return super.getAcsResponse(request, regionId);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getAcsResponse(request, regionId);
            } else {
                throw e;
            }
        }
    }

    @Override
    public CommonResponse getCommonResponse(CommonRequest request) throws ServerException, ClientException {
        try {
            return super.getCommonResponse(request);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.getCommonResponse(request);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts, IClientProfile profile) throws ClientException, ServerException {
        try {
            return super.doAction(request, autoRetry, maxRetryCounts, profile);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request, autoRetry, maxRetryCounts, profile);
            } else {
                throw e;
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryNumber, String regionId, Credential credential, Signer signer, FormatType format) throws ClientException, ServerException {
        try {
            return super.doAction(request, autoRetry, maxRetryNumber, regionId, credential, signer, format);
        } catch (ClientException e) {
            if (judgeTempAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
                return super.doAction(request, autoRetry, maxRetryNumber, regionId, credential, signer, format);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void shutdown() {
        shutdown(this);
    }

    protected void shutdown(ProxyDefaultSdkCoreClient client) {
        if (!isClosing) {
            isClosing = true;
            try {
                super.shutdown();
                secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(secretName, client);
            } catch (Exception e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:shutdown", e);
            }
        }
    }
}




