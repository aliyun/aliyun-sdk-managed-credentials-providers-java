package com.aliyun.kms.secretsmanager.plugin.common.auth;

import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;


public class SecretsManagerPluginCredentials implements CloudCredentials {

    private final String accessKeyId;
    private final String accessKeySecret;
    private final long expireTimestamp;

    public SecretsManagerPluginCredentials(String accessKeyId, String accessKeySecret, long expireTimestamp) {
        if (StringUtils.isEmpty(accessKeyId)) {
            throw new IllegalArgumentException("Access key ID cannot be null.");
        } else if (StringUtils.isEmpty(accessKeySecret)) {
            throw new IllegalArgumentException("Access key secret cannot be null.");
        } else {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
        }
        this.expireTimestamp = expireTimestamp;
    }

    @Override
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public long getExpireTimestamp() {
        return this.expireTimestamp;
    }

    public boolean isExpire() {
        if (this.expireTimestamp == Constants.NOT_SUPPORT_TAMP_AK_TIMESTAMP) {
            return false;
        }
        return System.currentTimeMillis() >= this.expireTimestamp;
    }
}
