package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.BatchConsumerBean;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsBatchConsumerBeanBuilder {

    public BatchConsumerBean build(Properties properties, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsBatchConsumerBean(properties, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
