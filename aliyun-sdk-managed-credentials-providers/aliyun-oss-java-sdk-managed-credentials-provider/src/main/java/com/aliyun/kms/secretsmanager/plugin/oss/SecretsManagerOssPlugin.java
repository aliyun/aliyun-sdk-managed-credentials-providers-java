package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.CloudClientBuilder;
import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;
import com.aliyun.kms.secretsmanager.plugin.oss.operations.*;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.*;
import com.aliyun.oss.internal.OSSDownloadOperation;
import com.aliyun.oss.internal.OSSUploadOperation;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.io.IOException;
import java.lang.reflect.Field;
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
            return new ProxyOSSClient(endpoint, provider, config, secretName,akExpireHandler);
        }
    }

    class ProxyOSSClient extends OSSClient {

        private String secretName;
        private AKExpireHandler akExpireHandler;
        private boolean isClosing;
        private final static String SERVICE_CLIENT_FIELD_NAME = "serviceClient";
        private final static String BUCKET_OPERATION_FIELD_NAME = "bucketOperation";
        private final static String OBJECT_OPERATION_FIELD_NAME = "objectOperation";
        private final static String MULTIPART_OPERATION_FIELD_NAME = "multipartOperation";
        private final static String CORS_OPERATION_FIELD_NAME = "corsOperation";
        private final static String UPLOAD_OPERATION_FIELD_NAME = "uploadOperation";
        private final static String DOWNLOAD_OPERATION_FIELD_NAME = "downloadOperation";
        private final static String LIVE_CHANNEL_OPERATION_FIELD_NAME = "liveChannelOperation";

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

        public ProxyOSSClient(String endpoint, CredentialsProvider credsProvider, ClientConfiguration config, String secretName, AKExpireHandler akExpireHandler) {
            super(endpoint, credsProvider, config);
            this.secretName = secretName;
            this.akExpireHandler = akExpireHandler;
            initOssOptions();
        }
        @Override
        public void setDownloadOperation(OSSDownloadOperation downloadOperation) {
            if (ProxyOSSDownloadOperation.class.isAssignableFrom(downloadOperation.getClass())) {
                super.setDownloadOperation(downloadOperation);
            } else {
                throw new IllegalArgumentException("Not support OSSDownloadOperation, please use ProxyOSSDownloadOperation");
            }
        }

        @Override
        public void setUploadOperation(OSSUploadOperation uploadOperation) {
            if (ProxyOSSUploadOperation.class.isAssignableFrom(uploadOperation.getClass())) {
                super.setUploadOperation(uploadOperation);
            } else {
                throw new IllegalArgumentException("Not support OSSUploadOperation, please use ProxyOSSUploadOperation");
            }
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

        private void initOssOptions() {
            Class<OSSClient> ossClientClass = OSSClient.class;
            try {
                Field serviceClientField = ossClientClass.getDeclaredField(ProxyOSSClient.SERVICE_CLIENT_FIELD_NAME);
                serviceClientField.setAccessible(true);
                Object serviceClient = serviceClientField.get(this);

                Field bucketOperationField = ossClientClass.getDeclaredField(BUCKET_OPERATION_FIELD_NAME);
                bucketOperationField.setAccessible(true);
                bucketOperationField.set(this, new ProxyOSSBucketOperation((ServiceClient) serviceClient, this, secretName, akExpireHandler));

                Field objectOperationField = ossClientClass.getDeclaredField(OBJECT_OPERATION_FIELD_NAME);
                objectOperationField.setAccessible(true);
                ProxyOSSObjectOperation proxyOSSObjectOperation = new ProxyOSSObjectOperation((ServiceClient) serviceClient, this, secretName, akExpireHandler);
                objectOperationField.set(this, proxyOSSObjectOperation);

                Field multipartOperationField = ossClientClass.getDeclaredField(MULTIPART_OPERATION_FIELD_NAME);
                multipartOperationField.setAccessible(true);
                ProxyOSSMultipartOperation proxyOSSMultipartOperation = new ProxyOSSMultipartOperation((ServiceClient) serviceClient, this, secretName, akExpireHandler);
                multipartOperationField.set(this, proxyOSSMultipartOperation);

                Field corsOperationField = ossClientClass.getDeclaredField(CORS_OPERATION_FIELD_NAME);
                corsOperationField.setAccessible(true);
                corsOperationField.set(this, new ProxyCORSOperation((ServiceClient) serviceClient, this, secretName, akExpireHandler));

                Field uploadOperationField = ossClientClass.getDeclaredField(UPLOAD_OPERATION_FIELD_NAME);
                uploadOperationField.setAccessible(true);
                uploadOperationField.set(this, new ProxyOSSUploadOperation(proxyOSSMultipartOperation));

                Field downloadOperationField = ossClientClass.getDeclaredField(DOWNLOAD_OPERATION_FIELD_NAME);
                downloadOperationField.setAccessible(true);
                downloadOperationField.set(this, new ProxyOSSDownloadOperation(proxyOSSObjectOperation));

                Field liveChannelOperationField = ossClientClass.getDeclaredField(LIVE_CHANNEL_OPERATION_FIELD_NAME);
                liveChannelOperationField.setAccessible(true);
                liveChannelOperationField.set(this, new ProxyLiveChannelOperation((ServiceClient) serviceClient, this, secretName, akExpireHandler));

                this.setEndpoint(getEndpoint().toString());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("not support such OSSClient implements");
            }
        }
    }
}


