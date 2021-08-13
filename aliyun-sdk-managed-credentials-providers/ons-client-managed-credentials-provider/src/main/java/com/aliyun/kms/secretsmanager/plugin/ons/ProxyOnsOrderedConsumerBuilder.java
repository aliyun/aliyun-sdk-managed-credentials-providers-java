package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.order.OrderConsumer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsOrderedConsumerBuilder {

    public OrderConsumer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsOrderedConsumer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
