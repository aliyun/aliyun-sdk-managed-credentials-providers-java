package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.bean.TransactionProducerBean;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsTransactionProducerBeanBuilder {

    public TransactionProducerBean build(Properties properties, LocalTransactionChecker checker, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsTransactionProducerBean(properties, checker, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
