package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.common.CloudClientBuilder;
import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.ons.api.batch.BatchConsumer;
import com.aliyun.openservices.ons.api.bean.*;
import com.aliyun.openservices.ons.api.impl.authority.SessionCredentials;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class SecretsManagerOnsPlugin {

    private static Set<Class<? extends SecretsManagerPluginCredentialUpdater>> pluginCredentialUpdaterSet = new HashSet();

    private AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    static {
        pluginCredentialUpdaterSet.add(OnsPluginCredentialUpdater.class);
    }

    /**
     * use for spring bean
     */
    public SecretsManagerOnsPlugin() {
    }

    public SecretsManagerOnsPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    /**
     * use for spring bean
     *
     * @param secretsManagerPlugin
     */
    public void setSecretsManagerPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    public Producer createOnsProducer(Properties properties, String secretName) throws CacheSecretException {
        return (Producer) createOnsAdmin(properties, ONSAdminType.CommonProducer, null, secretName);
    }

    public OrderProducer createOnsOrderProducer(Properties properties, String secretName) throws CacheSecretException {
        return (OrderProducer) createOnsAdmin(properties, ONSAdminType.OrderProducer, null, secretName);
    }

    public TransactionProducer createOnsTransactionProducer(Properties properties, LocalTransactionChecker checker, String secretName) throws CacheSecretException {
        return (TransactionProducer) createOnsAdmin(properties, ONSAdminType.TransactionProducer, checker, secretName);
    }

    public Consumer createOnsConsumer(Properties properties, String secretName) throws CacheSecretException {
        return (Consumer) createOnsAdmin(properties, ONSAdminType.CommonConsumer, null, secretName);
    }

    public BatchConsumer createOnsBatchConsumer(Properties properties, String secretName) throws CacheSecretException {
        return (BatchConsumer) createOnsAdmin(properties, ONSAdminType.BatchConsumer, null, secretName);
    }

    public OrderConsumer createOnsOrderedConsumer(Properties properties, String secretName) throws CacheSecretException {
        return (OrderConsumer) createOnsAdmin(properties, ONSAdminType.OrderConsumer, null, secretName);
    }

    public PullConsumer createOnsPullConsumer(Properties properties, String secretName) throws CacheSecretException {
        return (PullConsumer) createOnsAdmin(properties, ONSAdminType.PullConsumer, null, secretName);
    }

    public ProducerBean createOnsProducerBean(Properties properties, String secretName) throws CacheSecretException {
        return (ProducerBean) createOnsAdmin(properties, ONSAdminType.CommonProducerBean, null, secretName);
    }

    public OrderProducerBean createOnsOrderProducerBean(Properties properties, String secretName) throws CacheSecretException {
        return (OrderProducerBean) createOnsAdmin(properties, ONSAdminType.OrderProducerBean, null, secretName);
    }

    public TransactionProducerBean createOnsTransactionProducerBean(Properties properties, LocalTransactionChecker checker, String secretName) throws CacheSecretException {
        return (TransactionProducerBean) createOnsAdmin(properties, ONSAdminType.TransactionProducerBean, checker, secretName);
    }

    public ConsumerBean createOnsConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        return (ConsumerBean) createOnsAdmin(properties, ONSAdminType.CommonConsumerBean, null, secretName);
    }

    public BatchConsumerBean createOnsBatchConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        return (BatchConsumerBean) createOnsAdmin(properties, ONSAdminType.BatchConsumerBean, null, secretName);
    }

    public OrderConsumerBean createOnsOrderedConsumerBean(Properties properties, String secretName) throws CacheSecretException {
        return (OrderConsumerBean) createOnsAdmin(properties, ONSAdminType.OrderConsumerBean, null, secretName);
    }

    private Admin createOnsAdmin(Properties properties, ONSAdminType adminType, LocalTransactionChecker checker, String secretName) throws CacheSecretException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        SecretInfo secretInfo = secretsManagerPlugin.getSecretInfo(realSecretName);
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        MQClientBuilder mqClientBuilder = new MQClientBuilder(adminType, properties, credentials, checker);
        Admin onsAdmin = mqClientBuilder.build();
        OnsPluginCredentialUpdater onsPluginCredentialUpdater = new OnsPluginCredentialUpdater(onsAdmin);
        secretsManagerPlugin.registerSecretsManagerPluginUpdater(secretInfo.getSecretName(), onsPluginCredentialUpdater);
        return onsAdmin;
    }

    public void destroy() throws IOException {
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(pluginCredentialUpdaterSet);
    }

    public void closeOnsClient(Admin admin, String secretName) throws IOException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(realSecretName, admin);
    }

    private enum ONSAdminType {
        CommonProducer,
        OrderProducer,
        TransactionProducer,
        CommonConsumer,
        BatchConsumer,
        OrderConsumer,
        PullConsumer,
        CommonProducerBean,
        OrderProducerBean,
        TransactionProducerBean,
        CommonConsumerBean,
        BatchConsumerBean,
        OrderConsumerBean
    }

    class MQClientBuilder implements CloudClientBuilder<Admin> {


        private final ONSAdminType adminType;
        private final Properties properties;
        private final LocalTransactionChecker checker;

        public MQClientBuilder(ONSAdminType adminType, Properties properties, SecretsManagerPluginCredentials credentials, LocalTransactionChecker checker) {
            this.adminType = adminType;
            this.properties = new Properties(properties);
            this.checker = checker;
            this.properties.setProperty(SessionCredentials.AccessKey, credentials.getAccessKeyId());
            this.properties.setProperty(SessionCredentials.SecretKey, credentials.getAccessKeySecret());
        }

        @Override
        public Admin build() {
            switch (adminType) {
                case CommonProducer:
                    Producer producer = ONSFactory.createProducer(properties);
                    return producer;
                case OrderProducer:
                    OrderProducer orderProducer = ONSFactory.createOrderProducer(properties);
                    return orderProducer;
                case TransactionProducer:
                    TransactionProducer transactionProducer = ONSFactory.createTransactionProducer(properties, checker);
                    return transactionProducer;
                case CommonConsumer:
                    Consumer consumer = ONSFactory.createConsumer(properties);
                    return consumer;
                case BatchConsumer:
                    BatchConsumer batchConsumer = ONSFactory.createBatchConsumer(properties);
                    return batchConsumer;
                case OrderConsumer:
                    OrderConsumer orderConsumer = ONSFactory.createOrderedConsumer(properties);
                    return orderConsumer;
                case PullConsumer:
                    PullConsumer pullConsumer = ONSFactory.createPullConsumer(properties);
                    return pullConsumer;
                case CommonProducerBean:
                    ProducerBean producerBean = new ProducerBean();
                    producerBean.setProperties(this.properties);
                    return producerBean;
                case OrderProducerBean:
                    OrderProducerBean orderProducerBean = new OrderProducerBean();
                    orderProducerBean.setProperties(this.properties);
                    return orderProducerBean;
                case TransactionProducerBean:
                    TransactionProducerBean transactionProducerBean = new TransactionProducerBean();
                    transactionProducerBean.setProperties(this.properties);
                    transactionProducerBean.setLocalTransactionChecker(this.checker);
                    return transactionProducerBean;
                case CommonConsumerBean:
                    ConsumerBean consumerBean = new ConsumerBean();
                    consumerBean.setProperties(this.properties);
                    return consumerBean;
                case BatchConsumerBean:
                    BatchConsumerBean batchConsumerBean = new BatchConsumerBean();
                    batchConsumerBean.setProperties(this.properties);
                    return batchConsumerBean;
                case OrderConsumerBean:
                    OrderConsumerBean orderConsumerBean = new OrderConsumerBean();
                    orderConsumerBean.setProperties(this.properties);
                    return orderConsumerBean;
            }
            return null;
        }
    }

}
