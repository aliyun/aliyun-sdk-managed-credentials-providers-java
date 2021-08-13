package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OssPluginCredentialUpdater implements SecretsManagerPluginCredentialUpdater<OSSClient> {
    private OSSClient ossClient;
    private OssPluginCredentialsProvider provider;

    public OssPluginCredentialUpdater(OSSClient ossClient, OssPluginCredentialsProvider provider) {
        this.ossClient = ossClient;
        this.provider = provider;
    }

    @Override
    public OSSClient getClient() {
        return ossClient;
    }

    @Override
    public void updateCredential(SecretInfo secretInfo) {
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        this.provider.setCredentials(new DefaultCredentials(credentials.getAccessKeyId(), credentials.getAccessKeySecret()));
    }

    @Override
    public void close() throws IOException {
        if (this.ossClient != null) {
            this.ossClient.shutdown();
        }
    }
}
