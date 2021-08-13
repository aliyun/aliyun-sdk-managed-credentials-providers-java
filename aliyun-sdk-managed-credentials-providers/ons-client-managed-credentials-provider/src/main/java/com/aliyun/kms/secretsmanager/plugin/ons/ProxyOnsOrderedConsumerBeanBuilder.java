package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.OrderConsumerBean;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsOrderedConsumerBeanBuilder {

    public OrderConsumerBean build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsOrderedConsumerBean(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
