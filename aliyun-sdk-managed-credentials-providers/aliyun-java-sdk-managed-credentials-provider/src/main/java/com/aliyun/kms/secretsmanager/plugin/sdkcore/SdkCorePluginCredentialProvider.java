package com.aliyun.kms.secretsmanager.plugin.sdkcore;

import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;

public class SdkCorePluginCredentialProvider implements AlibabaCloudCredentialsProvider {

    private AlibabaCloudCredentials credentials;

    public SdkCorePluginCredentialProvider(AlibabaCloudCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public AlibabaCloudCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AlibabaCloudCredentials credentials) {
        this.credentials = credentials;
    }
}