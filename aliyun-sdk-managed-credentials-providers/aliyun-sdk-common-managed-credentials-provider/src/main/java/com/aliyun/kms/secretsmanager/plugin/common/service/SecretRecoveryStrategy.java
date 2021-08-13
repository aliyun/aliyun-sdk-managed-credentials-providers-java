package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

public interface SecretRecoveryStrategy {

    default SecretInfo recoveryGetSecret(String secretName) throws ClientException {
        return null;
    }
}
