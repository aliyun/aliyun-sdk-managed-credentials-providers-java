package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsOrderProducerBuilder {

    public OrderProducer build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsOrderProducer(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
