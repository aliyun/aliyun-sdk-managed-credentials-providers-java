package com.aliyun.kms.secretsmanager.plugin.common.model;

import com.aliyun.kms.secretsmanager.plugin.common.service.MonitorCacheSecretStoreStrategy;
import com.aliyun.kms.secretsmanager.plugin.common.service.SecretExchange;
import com.aliyun.kms.secretsmanager.plugin.common.service.SecretRecoveryStrategy;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.kms.secretsmanager.client.model.DKmsConfig;
import com.aliyuncs.kms.secretsmanager.client.model.RegionInfo;
import com.aliyuncs.kms.secretsmanager.client.service.RefreshSecretStrategy;

import java.util.List;
import java.util.Map;

public class SecretsManagerPluginCredentialsProvider {

    private AlibabaCloudCredentialsProvider credentialsProvider;

    private MonitorCacheSecretStoreStrategy refreshableCacheSecretStoreStrategy;

    private SecretExchange secretExchange;

    private SecretRecoveryStrategy secretRecoveryStrategy;

    private List<RegionInfo> regionInfoList;

    private List<String> secretNames;

    private RefreshSecretStrategy refreshSecretStrategy;

    private Long rotationInterval;

    private Long delayInterval;

    private Map<RegionInfo, DKmsConfig> dkmsConfigsMap;

    public AlibabaCloudCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public void setCredentialsProvider(AlibabaCloudCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public MonitorCacheSecretStoreStrategy getRefreshableCacheSecretStoreStrategy() {
        return this.refreshableCacheSecretStoreStrategy;
    }

    public void setRefreshableCacheSecretStoreStrategy(MonitorCacheSecretStoreStrategy refreshableCacheSecretStoreStrategy) {
        this.refreshableCacheSecretStoreStrategy = refreshableCacheSecretStoreStrategy;
    }

    public SecretExchange getSecretExchange() {
        return secretExchange;
    }

    public void setSecretExchange(SecretExchange secretExchange) {
        this.secretExchange = secretExchange;
    }

    public List<RegionInfo> getRegionInfoList() {
        return this.regionInfoList;
    }

    public void setRegionInfoList(List<RegionInfo> regionInfoList) {
        this.regionInfoList = regionInfoList;
    }


    public List<String> getSecretNames() {
        return this.secretNames;
    }

    public void setSecretNames(List<String> secretNames) {
        this.secretNames = secretNames;
    }

    public SecretRecoveryStrategy getSecretRecoveryStrategy() {
        return secretRecoveryStrategy;
    }

    public void setSecretRecoveryStrategy(SecretRecoveryStrategy secretRecoveryStrategy) {
        this.secretRecoveryStrategy = secretRecoveryStrategy;
    }

    public RefreshSecretStrategy getRefreshSecretStrategy() {
        return this.refreshSecretStrategy;
    }

    public void setRefreshSecretStrategy(RefreshSecretStrategy refreshSecretStrategy) {
        this.refreshSecretStrategy = refreshSecretStrategy;
    }

    public void setRotationInterval(long rotationInterval) {
        this.rotationInterval = rotationInterval;
    }

    public Long getRotationInterval() {
        return this.rotationInterval;
    }

    public Long getDelayInterval() {
        return this.delayInterval;
    }


    public void setDelayInterval(long delayInterval) {
        this.delayInterval = delayInterval;
    }


    public Map<RegionInfo, DKmsConfig> getDkmsConfigsMap() {
        return dkmsConfigsMap;
    }

    public void setDkmsConfigsMap(Map<RegionInfo, DKmsConfig> dkmsConfigsMap) {
        this.dkmsConfigsMap = dkmsConfigsMap;
    }
}
