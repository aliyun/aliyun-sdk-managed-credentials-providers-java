package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.openservices.ons.api.Admin;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PullConsumer;
import com.aliyun.openservices.ons.api.batch.BatchConsumer;
import com.aliyun.openservices.ons.api.bean.*;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.io.IOException;
import java.util.Properties;

public class SecretsManagerOnsPluginManager {

    private static SecretsManagerOnsPlugin client;

    public static Producer createOnsProducer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsProducer(properties, secretName);
    }

    public static OrderProducer createOnsOrderProducer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsOrderProducer(properties, secretName);
    }

    public static TransactionProducer createOnsTransactionProducer(Properties properties, LocalTransactionChecker checker, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsTransactionProducer(properties, checker, secretName);
    }

    public static Consumer createOnsConsumer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsConsumer(properties, secretName);
    }

    public static BatchConsumer createOnsBatchConsumer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsBatchConsumer(properties, secretName);
    }

    public static OrderConsumer createOnsOrderedConsumer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsOrderedConsumer(properties, secretName);
    }

    public static PullConsumer createOnsPullConsumer(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsPullConsumer(properties, secretName);
    }

    public static ProducerBean createOnsProducerBean(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsProducerBean(properties, secretName);
    }

    public static OrderProducerBean createOnsOrderProducerBean(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsOrderProducerBean(properties, secretName);
    }

    public static TransactionProducerBean createOnsTransactionProducerBean(Properties properties, LocalTransactionChecker checker, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsTransactionProducerBean(properties, checker, secretName);
    }

    public static ConsumerBean createOnsConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsConsumerBean(properties, secretName);
    }

    public static BatchConsumerBean createOnsBatchConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsBatchConsumerBean(properties, secretName);
    }

    public static OrderConsumerBean createOnsOrderedConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        initSecretsManagerPlugin();
        return client.createOnsOrderedConsumerBean(properties, secretName);
    }

    public static void closeOnsClient(Admin admin, String secretName) throws IOException {
        initSecretsManagerPlugin();
        client.closeOnsClient(admin, secretName);
    }

    public static void destroy() throws IOException {
        if (client != null) {
            client.destroy();
        }
    }

    private static void initSecretsManagerPlugin() {
        if (client == null) {
            synchronized (SecretsManagerOnsPluginManager.class) {
                if (client == null) {
                    try {
                        AliyunSDKSecretsManagerPluginsManager.init();
                        AliyunSDKSecretsManagerPlugin secretsManagerPlugin = AliyunSDKSecretsManagerPluginsManager.getSecretsManagerPlugin();
                        client = new SecretsManagerOnsPlugin(secretsManagerPlugin);
                    } catch (CacheSecretException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
