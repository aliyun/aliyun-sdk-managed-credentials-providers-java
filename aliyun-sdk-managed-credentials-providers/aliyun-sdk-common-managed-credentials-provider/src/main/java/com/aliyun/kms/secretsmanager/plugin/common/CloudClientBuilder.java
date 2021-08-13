package com.aliyun.kms.secretsmanager.plugin.common;

public interface CloudClientBuilder<T> {

    /**
     * 构建云产品Client
     *
     * @return
     * @throws Exception
     */
    T build() throws Exception;
}
