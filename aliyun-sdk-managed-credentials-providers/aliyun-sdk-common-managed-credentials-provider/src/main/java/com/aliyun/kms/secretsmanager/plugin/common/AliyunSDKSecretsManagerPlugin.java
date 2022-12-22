package com.aliyun.kms.secretsmanager.plugin.common;


import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.model.MonitorMessageInfo;
import com.aliyun.kms.secretsmanager.plugin.common.model.SecretsManagerPluginCredentialsProvider;
import com.aliyun.kms.secretsmanager.plugin.common.service.*;
import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.kms.secretsmanager.client.SecretCacheClient;
import com.aliyuncs.kms.secretsmanager.client.SecretCacheClientBuilder;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.DKmsConfig;
import com.aliyuncs.kms.secretsmanager.client.model.RegionInfo;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.service.DefaultSecretManagerClientBuilder;
import com.aliyuncs.kms.secretsmanager.client.service.FullJitterBackoffStrategy;
import com.aliyuncs.kms.secretsmanager.client.service.RefreshSecretStrategy;
import com.aliyuncs.kms.secretsmanager.client.service.UserAgentManager;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AliyunSDKSecretsManagerPlugin {

    private SecretCacheClient secretCacheClient;
    private SecretsManagerPluginCacheHook secretsManagerPluginCacheHook;
    private MonitorCacheSecretStoreStrategy cacheSecretStoreStrategy;
    private ConcurrentHashMap<String, TokenBucket> refreshTimestampMap = new ConcurrentHashMap<>();
    private AlibabaCloudCredentialsProvider credentialsProvider;
    private List<RegionInfo> regionInfoList;
    private Map<RegionInfo, DKmsConfig> dkmsConfigsMap;
    private List<String> secretNames;
    private SecretExchange secretExchange;
    private SecretRecoveryStrategy secretRecoveryStrategy;
    private SecretsManagerPluginCredentialsProviderLoader secretsManagerPluginCredentialsProviderLoader;
    private RefreshSecretStrategy refreshSecretStrategy;
    private volatile boolean isShutdown;


    public AliyunSDKSecretsManagerPlugin() {
        this.secretsManagerPluginCredentialsProviderLoader = new DefaultSecretsManagerPluginCredentialsProviderLoader();
        AliyunSDKSecretsManagerPluginsManager.registerClientInstance(this);
    }

    /**
     * use for spring bean
     */
    public AliyunSDKSecretsManagerPlugin(final SecretsManagerPluginCredentialsProviderLoader secretsManagerPluginCredentialsProviderLoader) {
        this.secretsManagerPluginCredentialsProviderLoader = secretsManagerPluginCredentialsProviderLoader;
        AliyunSDKSecretsManagerPluginsManager.registerClientInstance(this);
    }

    public void init() throws CacheSecretException {
        if (this.secretsManagerPluginCredentialsProviderLoader == null) {
            throw new IllegalArgumentException("the secret manager plugin credentials provider loader is null");
        }
        initLogger();
        UserAgentManager.registerUserAgent(Constants.SECRETSMANAGER_PLUGIN_JAVA_OF_USER_AGENT, Constants.SECRETSMANAGER_PLUGIN_JAVA_OF_USER_AGENT_PRIORITY, Constants.PROJECT_VERSION);
        SecretsManagerPluginCredentialsProvider secretsManagerPluginCredentialsProvider = this.secretsManagerPluginCredentialsProviderLoader.load();
        this.credentialsProvider = secretsManagerPluginCredentialsProvider.getCredentialsProvider();
        this.regionInfoList = secretsManagerPluginCredentialsProvider.getRegionInfoList();
        this.secretNames = secretsManagerPluginCredentialsProvider.getSecretNames();
        this.secretExchange = secretsManagerPluginCredentialsProvider.getSecretExchange();
        this.secretRecoveryStrategy = secretsManagerPluginCredentialsProvider.getSecretRecoveryStrategy();
        this.cacheSecretStoreStrategy = secretsManagerPluginCredentialsProvider.getRefreshableCacheSecretStoreStrategy();
        this.refreshSecretStrategy = secretsManagerPluginCredentialsProvider.getRefreshSecretStrategy();
        this.dkmsConfigsMap = secretsManagerPluginCredentialsProvider.getDkmsConfigsMap();
        if (this.dkmsConfigsMap == null && (this.credentialsProvider == null || this.regionInfoList == null)) {
            throw new IllegalArgumentException("the alibaba cloud credentials provider or region info list is null");
        }
        initSecretManagerClient();
        //异步刷新
        new Thread(() -> {
            for (String secretName : this.secretNames) {
                try {
                    this.refreshSecretInfo(secretName);
                } catch (Exception e) {
                    CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:refreshSecret", e);
                }
            }
        }).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.shutdown();
            } catch (IOException e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:shutdown", e);
            }
        }));
        CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).infof("SecretsManagerPlugin init success");
    }

    private void initLogger() {
        String baseDir = System.getProperty(Constants.LOGGER_BASE_DIR_KEY);
        if (baseDir == null) {
            baseDir = System.getProperty("user.dir");
            System.setProperty(Constants.LOGGER_BASE_DIR_KEY, baseDir);
        }
        CommonLogger.registerLogger(CacheClientConstant.MODE_NAME, LoggerFactory.getLogger(CacheClientConstant.MODE_NAME));
    }

    /**
     * 初始化Secret Manager Client
     *
     * @throws CacheSecretException
     */
    private void initSecretManagerClient() throws CacheSecretException {
        BlockingQueue<MonitorMessageInfo> blockingQueue = new LinkedBlockingQueue<>(1000);
        this.secretsManagerPluginCacheHook = new SecretsManagerPluginCacheHook(blockingQueue, this.secretRecoveryStrategy);
        DefaultSecretManagerClientBuilder clientBuilder = DefaultSecretManagerClientBuilder.standard();
        if (this.dkmsConfigsMap != null && !this.dkmsConfigsMap.isEmpty()) {
            for (DKmsConfig dKmsConfig : this.dkmsConfigsMap.values()) {
                clientBuilder.addDKmsConfig(dKmsConfig);
            }
        } else {
            clientBuilder.withCredentialsProvider(this.credentialsProvider);
            for (RegionInfo regionInfo : regionInfoList) {
                clientBuilder.addRegion(regionInfo);
            }
        }
        clientBuilder.withBackoffStrategy(new FullJitterBackoffStrategy(Constants.RETRY_MAX_ATTEMPTS, Constants.RETRY_INITIAL_INTERVAL_MILLS, Constants.CAPACITY));
        SecretCacheClientBuilder secretCacheClientBuilder = SecretCacheClientBuilder.newCacheClientBuilder(clientBuilder.build())
                .withRefreshSecretStrategy(this.refreshSecretStrategy)
                .withSecretCacheHook(this.secretsManagerPluginCacheHook).withLogger(LoggerFactory.getLogger(Constants.LOGGER_NAME));
        if (this.cacheSecretStoreStrategy == null) {
            this.cacheSecretStoreStrategy = new MonitorMemoryCacheSecretStoreStrategy();
        }
        this.cacheSecretStoreStrategy.addMonitorQueue(blockingQueue);
        secretCacheClientBuilder.withCacheSecretStrategy(this.cacheSecretStoreStrategy);
        this.secretCacheClient = secretCacheClientBuilder.build();
        this.cacheSecretStoreStrategy.addRefreshHook(this.secretCacheClient);
    }

    public SecretInfo getSecretInfo(String secretName) throws CacheSecretException {
        return secretCacheClient.getSecretInfo(secretName);
    }

    public SecretsManagerPluginCredentials getAccessKey(String secretName) throws CacheSecretException {
        SecretInfo secretInfo = getSecretInfo(secretName);
        return CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
    }

    public void registerSecretsManagerPluginUpdater(String secretName, SecretsManagerPluginCredentialUpdater secretsManagerPluginUpdater) {
        this.secretsManagerPluginCacheHook.registerSecretsManagerPluginUpdater(secretName, secretsManagerPluginUpdater);
    }

    public void closeSecretsManagerPluginUpdaterAndClient(String secretName, Object client) throws IOException {
        this.secretsManagerPluginCacheHook.closeSecretsManagerPluginUpdaterAndClient(secretName, client);
    }

    public void closeSecretsManagerPluginUpdaterAndClient(Set<Class<? extends SecretsManagerPluginCredentialUpdater>> updaterClasses) throws IOException {
        this.secretsManagerPluginCacheHook.closeSecretsManagerPluginUpdaterAndClient(updaterClasses);
    }

    public String getSecretName(String userSecretName) {
        return this.secretExchange.exchangeSecretName(userSecretName);
    }

    public void refreshSecretInfo(String secretName) {
        if (judgeRefreshSecretInfo(secretName)) {
            try {
                this.secretCacheClient.refreshNow(secretName);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:Refresh", e);
            }
        }
    }

    private boolean judgeRefreshSecretInfo(String secretName) {
        TokenBucket newTokenBucket = new TokenBucket(Constants.DEFAULT_MAX_TOKEN_NUMBER, Constants.DEFAULT_RATE_LIMIT_PERIOD);
        TokenBucket tokenBucket = this.refreshTimestampMap.putIfAbsent(secretName, newTokenBucket);
        if (tokenBucket != null) {
            return tokenBucket.hasQuota();
        }
        return newTokenBucket.hasQuota();
    }

    public void shutdown() throws IOException {
        if (!this.isShutdown) {
            this.isShutdown = true;
            try {
                secretCacheClient.close();
            } catch (IllegalStateException e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:shutdown", e);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("action:shutdown", e);
            }
        }
    }

    public void setSecretsManagerPluginCredentialsProviderLoader(SecretsManagerPluginCredentialsProviderLoader secretsManagerPluginCredentialsProviderLoader) {
        this.secretsManagerPluginCredentialsProviderLoader = secretsManagerPluginCredentialsProviderLoader;
    }

    class TokenBucket {

        private Lock lock = new ReentrantLock();

        public TokenBucket(long maxTokens, long rate) {
            this.maxTokens = maxTokens;
            this.currentTokens = maxTokens;
            this.rate = rate;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 当前的Token数量
         */
        private long currentTokens;

        /**
         * 最大容量
         */
        private long maxTokens;

        /**
         * 每隔多长时间,增加一个Token
         */
        private long rate;

        /**
         * 上次补充token时间
         */
        private long lastUpdateTime;

        boolean hasQuota() {
            this.lock.lock();
            try {
                long now = System.currentTimeMillis();
                long newTokens = (long) ((now - this.lastUpdateTime) * 1.0 / this.rate); //增加的Token数量
                if (newTokens > 0) {
                    this.lastUpdateTime = now;
                }
                this.currentTokens += newTokens;
                if (this.currentTokens > this.maxTokens) {
                    this.currentTokens = this.maxTokens;
                }
                long remaining = this.currentTokens - 1;
                if (remaining >= 0) {
                    this.currentTokens = remaining;
                    return true;
                }
            } finally {
                this.lock.unlock();
            }
            return false;
        }
    }
}
