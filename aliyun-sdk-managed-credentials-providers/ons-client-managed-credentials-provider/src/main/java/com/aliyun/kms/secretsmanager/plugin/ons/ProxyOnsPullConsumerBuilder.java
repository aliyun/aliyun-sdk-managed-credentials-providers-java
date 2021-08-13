package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.PullConsumer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsPullConsumerBuilder {

    public PullConsumer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsPullConsumer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
