package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.model.MonitorMessageInfo;
import com.aliyuncs.kms.secretsmanager.client.SecretCacheClient;
import com.aliyuncs.kms.secretsmanager.client.cache.CacheSecretStoreStrategy;

import java.util.concurrent.BlockingQueue;

public interface MonitorCacheSecretStoreStrategy extends CacheSecretStoreStrategy {

    /**
     * add refresh hook
     *
     * @param client
     */
    void addRefreshHook(SecretCacheClient client);

    /**
     * add the block queue with the monitor events
     *
     * @param blockingQueue
     */
    void addMonitorQueue(BlockingQueue<MonitorMessageInfo> blockingQueue);
}
