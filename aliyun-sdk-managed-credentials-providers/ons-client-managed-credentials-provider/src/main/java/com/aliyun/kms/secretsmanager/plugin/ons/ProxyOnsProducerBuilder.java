package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.Producer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsProducerBuilder {

    public Producer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsProducer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
