package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;

public class OssPluginCredentialsProvider implements CredentialsProvider {

    private Credentials credentials;

    public OssPluginCredentialsProvider(final Credentials credentials) {
        this.credentials = credentials;
    }


    @Override
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public Credentials getCredentials() {
        return this.credentials;
    }
}
