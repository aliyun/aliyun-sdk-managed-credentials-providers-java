package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.OrderProducerBean;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsOrderProducerBeanBuilder {

    public OrderProducerBean build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsOrderProducerBean(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
