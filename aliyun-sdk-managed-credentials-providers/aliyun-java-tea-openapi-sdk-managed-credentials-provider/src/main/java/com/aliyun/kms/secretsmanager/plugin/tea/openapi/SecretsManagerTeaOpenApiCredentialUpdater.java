package com.aliyun.kms.secretsmanager.plugin.tea.openapi;

import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.teaopenapi.Client;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.IOException;
import java.lang.reflect.Field;


public class SecretsManagerTeaOpenApiCredentialUpdater<T extends Client> implements SecretsManagerPluginCredentialUpdater<T> {

    private static final String _CREDENTIAL_FIELD_NAME = "_credential";

    private final T openApiClient;
    private final Field _credentialField;

    public SecretsManagerTeaOpenApiCredentialUpdater(T openApiClient) {
        this.openApiClient = openApiClient;
        try {
            _credentialField = this.openApiClient.getClass().getSuperclass().getSuperclass().getDeclaredField(_CREDENTIAL_FIELD_NAME);
            _credentialField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T getClient() {
        return openApiClient;
    }
    @Override
    public void updateCredential(SecretInfo secretInfo) {
        try {
            SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
            com.aliyun.credentials.models.Config config = new com.aliyun.credentials.models.Config();
            config.accessKeyId = credentials.getAccessKeyId();
            config.accessKeySecret = credentials.getAccessKeySecret();
            config.type = AuthConstant.ACCESS_KEY;
            _credentialField.set(this.openApiClient, new com.aliyun.credentials.Client(config));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}