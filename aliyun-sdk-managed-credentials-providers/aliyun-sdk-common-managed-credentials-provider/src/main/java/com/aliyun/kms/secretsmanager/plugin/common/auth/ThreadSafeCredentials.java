package com.aliyun.kms.secretsmanager.plugin.common.auth;


import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeCredentials implements CloudCredentials {

    private String accessKeyId;
    private String accessKeySecret;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ThreadSafeCredentials(String accessKeyId, String accessKeySecret) {
        if (StringUtils.isEmpty(accessKeyId)) {
            throw new IllegalArgumentException("Access key ID cannot be null.");
        } else if (StringUtils.isEmpty(accessKeySecret)) {
            throw new IllegalArgumentException("Access key secret cannot be null.");
        } else {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
        }
    }

    public void switchCredentials(SecretsManagerPluginCredentials secretsManagerPluginCredentials) {
        boolean addWriteLock = false;
        try {
            if (this.lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                addWriteLock = true;
            } else {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("common thread safe switchCredentials lock");
            }
            this.accessKeyId = secretsManagerPluginCredentials.getAccessKeyId();
            this.accessKeySecret = secretsManagerPluginCredentials.getAccessKeySecret();
        } catch (InterruptedException e) {
            new RuntimeException(e);
        } finally {
            if (addWriteLock) {
                this.lock.writeLock().unlock();
            }
        }
    }

    @Override
    public String getAccessKeyId() {
        this.lock.readLock().lock();
        String accessKeyId = this.accessKeyId;
        this.lock.readLock().unlock();
        return accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        this.lock.readLock().lock();
        String accessKeySecret = this.accessKeySecret;
        this.lock.readLock().unlock();
        return accessKeySecret;
    }
}
