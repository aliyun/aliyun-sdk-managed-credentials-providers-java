package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.model.SecretsManagerPluginCredentialsProvider;

public interface SecretsManagerPluginCredentialsProviderLoader {

    SecretsManagerPluginCredentialsProvider load();

    void setRotationInterval(long rotationInterval);

    void setDelayInterval(long delayInterval);
}
