package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.SecretsManagerPluginCredentialUpdater;
import com.aliyun.kms.secretsmanager.plugin.common.model.MonitorMessageInfo;
import com.aliyun.kms.secretsmanager.plugin.common.utils.MonitorMessageUtils;
import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.secretsmanager.client.cache.SecretCacheHook;
import com.aliyuncs.kms.secretsmanager.client.model.CacheSecretInfo;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class SecretsManagerPluginCacheHook implements SecretCacheHook {
    /**
     * secrets manager plugin updater map
     */
    private Map<String, List<SecretsManagerPluginCredentialUpdater>> secretsManagerPluginUpdaterMap;
    /**
     * the version stage of the cached secret
     */
    private String stage = Constants.KMS_SECRET_CURRENT_STAGE_VERSION;

    private BlockingQueue<MonitorMessageInfo> blockingQueue;
    private SecretRecoveryStrategy secretRecoveryStrategy;

    public SecretsManagerPluginCacheHook(BlockingQueue<MonitorMessageInfo> blockingQueue, SecretRecoveryStrategy secretRecoveryStrategy) {
        this.blockingQueue = blockingQueue;
        this.secretRecoveryStrategy = secretRecoveryStrategy;
    }

    @Override
    public void init() {
        secretsManagerPluginUpdaterMap = new HashMap<>();
    }

    @Override
    public CacheSecretInfo put(SecretInfo secretInfo) {
        String secretName = secretInfo.getSecretName();
        List<SecretsManagerPluginCredentialUpdater> updaterList = secretsManagerPluginUpdaterMap.get(secretName);
        if (updaterList != null) {
            for (SecretsManagerPluginCredentialUpdater updater : updaterList) {
                try {
                    updater.updateCredential(secretInfo);
                } catch (Throwable e) {
                    MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.UPDATE_CREDENTIAL_ACTION, secretName, null, e.getMessage(), true));
                }
            }
        }
        return new CacheSecretInfo(secretInfo, stage, System.currentTimeMillis());
    }

    @Override
    public SecretInfo get(CacheSecretInfo cacheSecretInfo) {
        return cacheSecretInfo.getSecretInfo();
    }

    @Override
    public SecretInfo recoveryGetSecret(String secretName) throws ClientException {
        SecretInfo secretInfo = this.secretRecoveryStrategy.recoveryGetSecret(secretName);
        if (secretInfo != null) {
            MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.RECOVERY_GET_SECRET_ACTION, secretName, null, String.format("The secret named [%s] recovery success", secretName), true));
            return secretInfo;
        }
        MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.RECOVERY_GET_SECRET_ACTION, secretName, null, String.format("The secret named [%s] recovery fail", secretName), true));
        return null;
    }

    public void registerSecretsManagerPluginUpdater(String secretName, SecretsManagerPluginCredentialUpdater secretsManagerPluginUpdater) {
        List<SecretsManagerPluginCredentialUpdater> updaterList = secretsManagerPluginUpdaterMap.get(secretName);
        if (updaterList == null) {
            synchronized (secretsManagerPluginUpdaterMap) {
                updaterList = secretsManagerPluginUpdaterMap.get(secretName);
                if (updaterList == null) {
                    secretsManagerPluginUpdaterMap.put(secretName, new CopyOnWriteArrayList<>());
                }
            }
            updaterList = secretsManagerPluginUpdaterMap.get(secretName);
        }
        updaterList.add(secretsManagerPluginUpdater);
    }

    public void closeSecretsManagerPluginUpdaterAndClient(String secretName, Object client) throws IOException {
        List<SecretsManagerPluginCredentialUpdater> updaterList = secretsManagerPluginUpdaterMap.get(secretName);
        for (SecretsManagerPluginCredentialUpdater updater : updaterList) {
            if (updater.getClient() == client) {
                updaterList.remove(updater);
                updater.close();
            }
        }
    }

    public void closeSecretsManagerPluginUpdaterAndClient(Set<Class<? extends SecretsManagerPluginCredentialUpdater>> updaterClasses) throws IOException {
        for (List<SecretsManagerPluginCredentialUpdater> credentialUpdaters : secretsManagerPluginUpdaterMap.values()) {
            for (SecretsManagerPluginCredentialUpdater credentialUpdater : credentialUpdaters) {
                if (updaterClasses.contains(credentialUpdater.getClass())) {
                    credentialUpdaters.remove(credentialUpdater);
                    credentialUpdater.close();
                }
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (secretsManagerPluginUpdaterMap.size() > 0) {
            for (List<SecretsManagerPluginCredentialUpdater> credentialUpdaters : secretsManagerPluginUpdaterMap.values()) {
                for (SecretsManagerPluginCredentialUpdater credentialUpdater : credentialUpdaters) {
                    credentialUpdaters.remove(credentialUpdater);
                    credentialUpdater.close();
                }
            }
        }
    }
}
