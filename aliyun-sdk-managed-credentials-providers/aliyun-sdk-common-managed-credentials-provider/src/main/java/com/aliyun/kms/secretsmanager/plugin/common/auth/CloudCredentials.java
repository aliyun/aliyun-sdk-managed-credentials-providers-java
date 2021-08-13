package com.aliyun.kms.secretsmanager.plugin.common.auth;

public interface CloudCredentials {

    /**
     * obtain access key id
     * @return
     */
    String getAccessKeyId();

    /**
     * obtain access key secret
     * @return
     */
    String getAccessKeySecret();
}
