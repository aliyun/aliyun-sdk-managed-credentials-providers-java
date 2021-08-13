package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsConsumerBeanBuilder {

    public ConsumerBean build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsConsumerBean(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
