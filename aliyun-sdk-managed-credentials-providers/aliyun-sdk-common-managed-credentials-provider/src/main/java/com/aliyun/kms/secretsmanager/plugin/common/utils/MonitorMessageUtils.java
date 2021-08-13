package com.aliyun.kms.secretsmanager.plugin.common.utils;

import com.aliyun.kms.secretsmanager.plugin.common.model.MonitorMessageInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.util.concurrent.BlockingQueue;

public class MonitorMessageUtils {

    private MonitorMessageUtils() {
        // do nothing
    }

    public static void addMessage(BlockingQueue<MonitorMessageInfo> blockingQueue, MonitorMessageInfo monitorMessageInfo) {
        if (!blockingQueue.offer(monitorMessageInfo)) {
            CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("execute {} fail with secret[{}] and error[{}]", monitorMessageInfo.getAction(), monitorMessageInfo.getSecretName(), monitorMessageInfo.getErrorMessage());
        }
    }
}
