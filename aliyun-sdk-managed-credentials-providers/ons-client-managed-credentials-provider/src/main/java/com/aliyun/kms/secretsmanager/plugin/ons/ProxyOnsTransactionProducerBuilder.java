package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.Properties;

public class ProxyOnsTransactionProducerBuilder {

    public TransactionProducer build(Properties properties, LocalTransactionChecker checker, String secretName) {
        try {
            return SecretsManagerOnsPluginManager.createOnsTransactionProducer(properties, checker, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }
}
