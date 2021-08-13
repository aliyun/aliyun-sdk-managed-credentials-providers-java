package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.model.SecretsManagerPluginCredentialsProvider;
import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.auth.InstanceProfileCredentialsProvider;
import com.aliyuncs.kms.secretsmanager.client.model.ClientKeyCredentialsProvider;
import com.aliyuncs.kms.secretsmanager.client.model.CredentialsProperties;
import com.aliyuncs.kms.secretsmanager.client.utils.CredentialsPropertiesUtils;

public class DefaultSecretsManagerPluginCredentialsProviderLoader implements SecretsManagerPluginCredentialsProviderLoader {

    private SecretsManagerPluginCredentialsProvider secretsManagerPluginCredentialsProvider;


    public DefaultSecretsManagerPluginCredentialsProviderLoader() {
        this.secretsManagerPluginCredentialsProvider = new SecretsManagerPluginCredentialsProvider();
    }

    public DefaultSecretsManagerPluginCredentialsProviderLoader(SecretsManagerPluginCredentialsProvider secretsManagerPluginCredentialsProvider) {
        this.secretsManagerPluginCredentialsProvider = secretsManagerPluginCredentialsProvider;
    }

    @Override
    public SecretsManagerPluginCredentialsProvider load() {
        CredentialsProperties credentialsProperties = CredentialsPropertiesUtils.loadCredentialsProperties(Constants.DEFAULT_CREDENTIAL_PROPERTIES_FILE_NAME);
        long monitorPeriodMilliseconds;
        long monitorCustomerMilliseconds;
        if (credentialsProperties != null) {
            monitorPeriodMilliseconds = Long.parseLong(String.valueOf(credentialsProperties.getSourceProperties().getOrDefault(Constants.PROPERTIES_MONITOR_PERIOD_MILLISECONDS_KEY, 0)));
            monitorCustomerMilliseconds = Long.parseLong(String.valueOf(credentialsProperties.getSourceProperties().getOrDefault(Constants.PROPERTIES_MONITOR_CUSTOMER_MILLISECONDS_KEY, 0)));
            if (this.secretsManagerPluginCredentialsProvider.getRotationInterval() == null) {
                this.secretsManagerPluginCredentialsProvider.setRotationInterval(Long.parseLong(String.valueOf(credentialsProperties.getSourceProperties().getOrDefault(Constants.PROPERTIES_ROTATION_INTERVAL_KEY, 0))));
            }
            if (this.secretsManagerPluginCredentialsProvider.getDelayInterval() == null) {
                this.secretsManagerPluginCredentialsProvider.setDelayInterval(Long.parseLong(String.valueOf(credentialsProperties.getSourceProperties().getOrDefault(Constants.PROPERTIES_DELAY_INTERVAL_KEY, 0))));
            }
        } else {
            throw new IllegalArgumentException("credentials properties is invalid");
        }
        AlibabaCloudCredentialsProvider credentialsProvider = credentialsProperties.getProvider();
        checkCredentialsProvider(credentialsProvider);
        this.secretsManagerPluginCredentialsProvider.setCredentialsProvider(credentialsProvider);
        this.secretsManagerPluginCredentialsProvider.setSecretExchange(new SecretExchange() {
        });
        this.secretsManagerPluginCredentialsProvider.setSecretRecoveryStrategy(new SecretRecoveryStrategy() {
        });
        this.secretsManagerPluginCredentialsProvider.setRegionInfoList(credentialsProperties.getRegionInfoList());
        this.secretsManagerPluginCredentialsProvider.setSecretNames(credentialsProperties.getSecretNameList());
        this.secretsManagerPluginCredentialsProvider.setRefreshableCacheSecretStoreStrategy(new MonitorMemoryCacheSecretStoreStrategy(monitorPeriodMilliseconds, monitorCustomerMilliseconds));
        this.secretsManagerPluginCredentialsProvider.setRefreshSecretStrategy(new RotateAKSecretRefreshSecretStrategy(this.secretsManagerPluginCredentialsProvider.getRotationInterval(), this.secretsManagerPluginCredentialsProvider.getDelayInterval()));
        return this.secretsManagerPluginCredentialsProvider;
    }

    private void checkCredentialsProvider(AlibabaCloudCredentialsProvider credentialsProvider) {
        if (credentialsProvider == null) {
            throw new IllegalArgumentException("credentials provider must be provided");
        }
        if (!(credentialsProvider instanceof InstanceProfileCredentialsProvider) && !(credentialsProvider instanceof ClientKeyCredentialsProvider)) {
            throw new IllegalArgumentException("credentials provider must be ecs instance ram role or client key");
        }
    }

    @Override
    public void setRotationInterval(long rotationInterval) {
        this.secretsManagerPluginCredentialsProvider.setRotationInterval(rotationInterval);
    }

    @Override
    public void setDelayInterval(long delayInterval) {
        this.secretsManagerPluginCredentialsProvider.setDelayInterval(delayInterval);
    }

}
