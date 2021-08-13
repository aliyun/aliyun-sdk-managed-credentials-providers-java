package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.ProducerBean;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsProducerBeanBuilder {

    public ProducerBean build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsProducerBean(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
