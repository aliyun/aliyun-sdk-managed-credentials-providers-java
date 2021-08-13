package com.aliyun.kms.secretsmanager.plugin.common;

import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.Closeable;

public interface SecretsManagerPluginCredentialUpdater<T> extends Closeable {

    /**
     * 获取云产品Client
     *
     * @return
     */
    T getClient();


    /**
     * 更新TmpAK信息
     *
     * @param secretInfo
     */
    void updateCredential(SecretInfo secretInfo);
}
