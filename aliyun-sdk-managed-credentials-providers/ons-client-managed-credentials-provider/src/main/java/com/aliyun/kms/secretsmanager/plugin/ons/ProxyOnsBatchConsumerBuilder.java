package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.batch.BatchConsumer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsBatchConsumerBuilder {

    public BatchConsumer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsBatchConsumer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
