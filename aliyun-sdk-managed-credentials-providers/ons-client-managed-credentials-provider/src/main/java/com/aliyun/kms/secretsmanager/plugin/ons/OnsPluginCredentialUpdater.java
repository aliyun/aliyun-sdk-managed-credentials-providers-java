package com.aliyun.kms.secretsmanager.plugin.ons;

import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.openservices.ons.api.Admin;
import com.aliyun.openservices.ons.api.impl.authority.SessionCredentials;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.IOException;
import java.util.Properties;

public class OnsPluginCredentialUpdater implements SecretsManagerPluginCredentialUpdater<Admin> {

    private final Admin adminClient;

    public OnsPluginCredentialUpdater(Admin adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public Admin getClient() {
        return adminClient;
    }

    @Override
    public void updateCredential(SecretInfo secretInfo) {
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        Properties properties = new Properties();
        properties.setProperty(SessionCredentials.AccessKey, credentials.getAccessKeyId());
        properties.setProperty(SessionCredentials.SecretKey, credentials.getAccessKeySecret());
        this.adminClient.updateCredential(properties);
    }

    @Override
    public void close() throws IOException {
        if (adminClient != null) {
            adminClient.shutdown();
        }
    }

}
