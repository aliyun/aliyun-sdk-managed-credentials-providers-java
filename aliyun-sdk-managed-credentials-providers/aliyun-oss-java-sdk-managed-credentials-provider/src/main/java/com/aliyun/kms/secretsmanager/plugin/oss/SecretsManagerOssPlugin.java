package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.CloudClientBuilder;
import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.RetryStrategy;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SecretsManagerOssPlugin {

    private static Set<Class<? extends SecretsManagerPluginCredentialUpdater>> pluginCredentialUpdaterSet = new HashSet();

    private AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    private AKExpireHandler akExpireHandler;

    static {
        pluginCredentialUpdaterSet.add(OssPluginCredentialUpdater.class);
    }

    /**
     * use for spring bean
     */
    public SecretsManagerOssPlugin() {
    }

    public SecretsManagerOssPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    public SecretsManagerOssPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin, AKExpireHandler akExpireHandler) {
        this.secretsManagerPlugin = secretsManagerPlugin;
        this.akExpireHandler = akExpireHandler;
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
    public void setAkExpireHandler(AKExpireHandler akExpireHandler) {
        this.akExpireHandler = akExpireHandler;
    }

    public OSS getOssClient(String endpoint, String secretName) throws CacheSecretException {
        return getOssClient(endpoint, new ClientConfiguration(), secretName);
    }

    public OSS getOssClient(String endpoint, String secretName, AKExpireHandler akExpireHandler) throws CacheSecretException {
        return getOssClient(endpoint, new ClientConfiguration(), secretName, akExpireHandler);
    }

    public OSS getOssClient(String endpoint, ClientConfiguration config, String secretName) throws CacheSecretException {
        return getOssClient(endpoint, config, secretName, null);
    }

    public OSS getOssClient(String endpoint, ClientConfiguration config, String secretName, AKExpireHandler akExpireHandler) throws CacheSecretException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        SecretInfo secretInfo = secretsManagerPlugin.getSecretInfo(realSecretName);
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        OssPluginCredentialsProvider provider = new OssPluginCredentialsProvider(new com.aliyun.oss.common.auth.DefaultCredentials(credentials.getAccessKeyId(), credentials.getAccessKeySecret()));
        if (akExpireHandler == null) {
            akExpireHandler = this.akExpireHandler;
        }
        OSSClient ossClient = new OssClientBuilder(endpoint, secretName, provider, config, akExpireHandler).build();
        OssPluginCredentialUpdater ossClientSecurityUpdater = new OssPluginCredentialUpdater(ossClient, provider);
        secretsManagerPlugin.registerSecretsManagerPluginUpdater(secretInfo.getSecretName(), ossClientSecurityUpdater);
        return ossClient;
    }

    public void closeOssClient(OSS ossClient, String secretName) throws IOException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(realSecretName, ossClient);
    }

    public void destroy() throws IOException {
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(pluginCredentialUpdaterSet);
    }

    class DefaultRetryStrategy extends RetryStrategy implements AKExpireHandler<Exception> {

        private final static String InvalidAccessKeyIdErr = "InvalidAccessKeyId";

        private final String secretName;
        private final RetryStrategy defaultRetryStrategy;
        private final AKExpireHandler akExpireHandler;

        public DefaultRetryStrategy(String secretName, RetryStrategy defaultRetryStrategy, AKExpireHandler akExpireHandler) {
            this.secretName = secretName;
            this.defaultRetryStrategy = defaultRetryStrategy;
            this.akExpireHandler = akExpireHandler;
        }

        @Override
        public boolean shouldRetry(Exception e, RequestMessage requestMessage, ResponseMessage responseMessage, int i) {
            if (akExpireHandler != null) {
                return shouldRetryWithHandler(e, requestMessage, responseMessage, i, akExpireHandler);
            } else {
                return shouldRetryWithHandler(e, requestMessage, responseMessage, i, this);
            }
        }

        boolean shouldRetryWithHandler(Exception e, RequestMessage requestMessage, ResponseMessage responseMessage, int i, AKExpireHandler akExpireHandler) {
            if (akExpireHandler.judgeAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
            } else if (defaultRetryStrategy != null) {
                return defaultRetryStrategy.shouldRetry(e, requestMessage, responseMessage, i);
            }
            return false;
        }

        @Override
        public boolean judgeAKExpire(Exception e) {
            if (e instanceof OSSException) {
                OSSException ossException = (OSSException) e;
                if (InvalidAccessKeyIdErr.equals(ossException.getErrorCode())) {
                    return true;
                }
            }
            return false;
        }
    }

    class OssClientBuilder implements CloudClientBuilder<OSSClient> {

        private final String endpoint;
        private final String secretName;
        private final OssPluginCredentialsProvider provider;
        private final AKExpireHandler akExpireHandler;

        private ClientConfiguration config;

        public OssClientBuilder(String endpoint, String secretName, OssPluginCredentialsProvider provider, AKExpireHandler akExpireHandler) {
            this(endpoint, secretName, provider, null, akExpireHandler);
        }

        public OssClientBuilder(String endpoint, String secretName, OssPluginCredentialsProvider provider, ClientConfiguration config, AKExpireHandler akExpireHandler) {
            this.endpoint = endpoint;
            this.secretName = secretName;
            this.provider = provider;
            this.akExpireHandler = akExpireHandler;
            this.config = config;
        }

        @Override
        public OSSClient build() {
            if (StringUtils.isEmpty(endpoint)) {
                throw new IllegalArgumentException("Missing parameter endpoint");
            }
            if (config == null) {
                config = new ClientConfiguration();
            }
            config.setRetryStrategy(new DefaultRetryStrategy(secretName, config.getRetryStrategy(), akExpireHandler));
            return new ProxyOSSClient(endpoint, provider, config, secretName);
        }
    }

    class ProxyOSSClient extends OSSClient {

        private String secretName;
        private boolean isClosing;

        public ProxyOSSClient(String accessKeyId, String secretAccessKey) {
            super(accessKeyId, secretAccessKey);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, String accessKeyId, String secretAccessKey) {
            super(endpoint, accessKeyId, secretAccessKey);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, String accessKeyId, String secretAccessKey, String securityToken) {
            super(endpoint, accessKeyId, secretAccessKey, securityToken);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, String accessKeyId, String secretAccessKey, ClientConfiguration config) {
            super(endpoint, accessKeyId, secretAccessKey, config);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, String accessKeyId, String secretAccessKey, String securityToken, ClientConfiguration config) {
            super(endpoint, accessKeyId, secretAccessKey, securityToken, config);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, CredentialsProvider credsProvider) {
            super(endpoint, credsProvider);
            throw new UnsupportedOperationException("Not support such constructors");
        }

        public ProxyOSSClient(String endpoint, CredentialsProvider credsProvider, ClientConfiguration config, String secretName) {
            super(endpoint, credsProvider, config);
            this.secretName = secretName;
        }

        @Override
        public void shutdown() {
            if (!isClosing) {
                isClosing = true;
                try {
                    super.shutdown();
                    SecretsManagerOssPluginManager.closeOssClient(this, secretName);
                } catch (IOException e) {
                    CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:shutdown", e);
                }
            }
        }
    }
}


