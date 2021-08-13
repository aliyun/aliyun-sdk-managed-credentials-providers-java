package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

public class ProxyOSSClientBuilder {

    public OSS build(String endpoint, String secretName) {
        return build(endpoint, null, secretName, null);
    }

    public OSS build(String endpoint, String secretName, AKExpireHandler akExpireHandler) {
        return build(endpoint,null, secretName,  akExpireHandler);
    }

    public OSS build(String endpoint, ClientBuilderConfiguration configuration, String secretName) {
        return build(endpoint, configuration, secretName, null);
    }

    public OSS build(String endpoint, ClientBuilderConfiguration configuration, String secretName, AKExpireHandler akExpireHandler) {
        try {
            return SecretsManagerOssPluginManager.getOssClient(endpoint, configuration, secretName, akExpireHandler);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }

}
