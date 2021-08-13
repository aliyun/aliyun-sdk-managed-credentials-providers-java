package com.aliyun.kms.secretsmanager.plugin.sdkcore;

import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.BasicCredentials;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;


import java.io.IOException;

public class SdkCorePluginCredentialUpdater implements SecretsManagerPluginCredentialUpdater<IAcsClient> {

    private final IAcsClient client;
    private final SdkCorePluginCredentialProvider provider;

    public SdkCorePluginCredentialUpdater(IAcsClient client, SdkCorePluginCredentialProvider provider) {
        this.client = client;
        this.provider = provider;
    }

    @Override
    public IAcsClient getClient() {
        return client;
    }

    @Override
    public void updateCredential(SecretInfo secretInfo) {
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        this.provider.setCredentials(new BasicCredentials(credentials.getAccessKeyId(), credentials.getAccessKeySecret()));
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.shutdown();
        }
    }
}
