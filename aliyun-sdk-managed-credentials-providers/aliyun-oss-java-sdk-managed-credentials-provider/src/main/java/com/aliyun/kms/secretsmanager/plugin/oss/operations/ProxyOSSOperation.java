package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.kms.secretsmanager.plugin.oss.SecretsManagerOssPluginManager;
import com.aliyun.oss.OSSException;

public interface ProxyOSSOperation {
    String InvalidAccessKeyIdErr = "InvalidAccessKeyId";
    String UnknownErr = "Unknown";
    String NoBodyInResponseErrMsg = "No body in response, http status code 403";
    AliyunSDKSecretsManagerPlugin secretsManagerPlugin = SecretsManagerOssPluginManager.getSecretsManagerPlugin();

    default void checkAndRefreshSecretInfo(OSSException e, String secretName, AKExpireHandler akExpireHandler, AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        if (akExpireHandler == null) {
            if (InvalidAccessKeyIdErr.equalsIgnoreCase(e.getErrorCode()) || (UnknownErr.equalsIgnoreCase(e.getErrorCode()) && e.getErrorMessage() != null && e.getErrorMessage().equals(NoBodyInResponseErrMsg))) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
            } else {
                throw e;
            }
        } else {
            if (akExpireHandler.judgeAKExpire(e)) {
                secretsManagerPlugin.refreshSecretInfo(secretName);
            } else {
                throw e;
            }
        }
    }
}
