package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.Consumer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsConsumerBuilder {

    public Consumer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsConsumer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
