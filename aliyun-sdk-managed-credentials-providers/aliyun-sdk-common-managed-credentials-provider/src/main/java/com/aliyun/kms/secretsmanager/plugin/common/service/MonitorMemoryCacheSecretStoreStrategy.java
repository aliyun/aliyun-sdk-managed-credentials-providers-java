package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.model.MonitorMessageInfo;
import com.aliyun.kms.secretsmanager.plugin.common.utils.MonitorMessageUtils;
import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;
import com.aliyuncs.kms.secretsmanager.client.SecretCacheClient;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.CacheSecretInfo;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;
import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class MonitorMemoryCacheSecretStoreStrategy implements MonitorCacheSecretStoreStrategy {

    private static final Gson gson = new Gson();

    private final Map<String, CacheSecretInfo> cacheSecretInfoMap = new ConcurrentHashMap();
    private final SecretsManagerPluginMonitor secretsManagerPluginMonitor;

    private SecretCacheClient secretCacheClient;

    public MonitorMemoryCacheSecretStoreStrategy(long monitorPeriod, long monitorCustomerPeriod) {
        this.secretsManagerPluginMonitor = new SecretsManagerPluginMonitor(monitorPeriod, monitorCustomerPeriod);
    }

    public MonitorMemoryCacheSecretStoreStrategy() {
        this.secretsManagerPluginMonitor = new SecretsManagerPluginMonitor();
    }

    @Override
    public void addRefreshHook(SecretCacheClient client) {
        this.secretCacheClient = client;
    }

    @Override
    public void addMonitorQueue(BlockingQueue<MonitorMessageInfo> blockingQueue) {
        this.secretsManagerPluginMonitor.setMonitorQueue(blockingQueue);
    }

    @Override
    public void init() throws CacheSecretException {
        this.secretsManagerPluginMonitor.init();
    }

    @Override
    public void storeSecret(CacheSecretInfo cacheSecretInfo) {
        this.cacheSecretInfoMap.put(cacheSecretInfo.getSecretInfo().getSecretName(), cacheSecretInfo);
    }

    @Override
    public CacheSecretInfo getCacheSecretInfo(String secretName) {
        return this.cacheSecretInfoMap.get(secretName);
    }

    @Override
    public void close() throws IOException {
        if (this.cacheSecretInfoMap != null) {
            this.cacheSecretInfoMap.clear();
        }
        if (this.secretsManagerPluginMonitor != null) {
            this.secretsManagerPluginMonitor.close();
        }
    }

    private class SecretsManagerPluginMonitor implements Closeable {
        private final static long DEFAULT_MONITOR_PERIOD = 30 * 60 * 1000;
        private final static long DEFAULT_MONITOR_CUSTOMER_PERIOD = 120 * 60 * 1000;

        private long monitorPeriod;
        private long monitorCustomerPeriod;
        private BlockingQueue<MonitorMessageInfo> blockingQueue = new LinkedBlockingQueue<>(1000);
        private ScheduledExecutorService scheduledExecutorService;

        public SecretsManagerPluginMonitor() {
        }

        public SecretsManagerPluginMonitor(long monitorPeriod, long monitorCustomerPeriod) {
            this.monitorPeriod = monitorPeriod;
            this.monitorCustomerPeriod = monitorCustomerPeriod;
        }

        public void setMonitorQueue(BlockingQueue<MonitorMessageInfo> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        public void init() {
            if (monitorPeriod < DEFAULT_MONITOR_PERIOD) {
                monitorPeriod = DEFAULT_MONITOR_PERIOD;
            }
            if (monitorCustomerPeriod < DEFAULT_MONITOR_CUSTOMER_PERIOD) {
                monitorCustomerPeriod = DEFAULT_MONITOR_CUSTOMER_PERIOD;
            }
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            Random random = new Random();
            long start = random.nextInt((int) DEFAULT_MONITOR_PERIOD);
            scheduledExecutorService.scheduleAtFixedRate(new MonitorTask(blockingQueue), start, monitorPeriod, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(new CustomerTask(blockingQueue), start, monitorCustomerPeriod, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() throws IOException {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdown();
            }
        }
    }

    private class CustomerTask implements Runnable {


        private final BlockingQueue<MonitorMessageInfo> blockingQueue;

        public CustomerTask(BlockingQueue<MonitorMessageInfo> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            try {
                while (blockingQueue.size() > 0) {
                    MonitorMessageInfo monitorMessageInfo = blockingQueue.remove();
                    CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).warnf("SecretsManagerPluginMonitor occur some problems secretName:{},action:{},errorMessage:{},timestamp:{} ",
                            monitorMessageInfo.getSecretName(), monitorMessageInfo.getAction(), monitorMessageInfo.getErrorMessage(), monitorMessageInfo.getTimestamp());
                }
            } catch (Exception e) {
                MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.CUSTOM_MESSAGE_ACTION, null, null, e.getMessage(), true));
            }
        }

    }

    private class MonitorTask implements Runnable {

        private final BlockingQueue<MonitorMessageInfo> blockingQueue;

        public MonitorTask(BlockingQueue<MonitorMessageInfo> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            for (String secretName : cacheSecretInfoMap.keySet()) {
                try {
                    monitorTempAKStatus(secretName);
                } catch (Exception e) {
                    MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.MONITOR_AK_STATUS_ACTION, secretName, null, String.format("Secret[%s] monitor occur exception:[%s]", secretName, e.getMessage()), true));
                }
            }
        }

        private void monitorTempAKStatus(String secretName) throws CacheSecretException {
            SecretInfo secretInfo = secretCacheClient.getSecretInfo(secretName);
            if (!StringUtils.isEmpty(secretInfo.getSecretType()) && Constants.RAM_CREDENTIALS_SECRET_TYPE.equalsIgnoreCase(secretInfo.getSecretType())) {
                String extendedConfig = secretInfo.getExtendedConfig();
                if (extendedConfig != null) {
                    Map<String, String> map = gson.fromJson(extendedConfig, Map.class);
                    String secretSubType = map.getOrDefault(Constants.EXTENDED_CONFIG_PROPERTY_SECRET_SUB_TYPE, "");
                    if (Constants.RAM_USER_ACCESS_KEY_SECRET_SUB_TYPE.equals(secretSubType)) {
                        CacheSecretInfo cacheSecretInfo = cacheSecretInfoMap.get(secretName);
                        if (judgeSecretExpired(secretInfo.getRotationInterval(), cacheSecretInfo.getRefreshTimestamp())) {
                            try {
                                boolean finished = secretCacheClient.refreshNow(secretName);
                                if (!finished) {
                                    MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.MONITOR_AK_STATUS_ACTION, secretName, null, String.format("Secret[%s] ak expire and fail to refresh", secretName), true));
                                } else {
                                    MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.MONITOR_AK_STATUS_ACTION, secretName, null, String.format("Secret[%s] ak expire,but success to refresh", secretName)));
                                }
                            } catch (InterruptedException e) {
                                MonitorMessageUtils.addMessage(this.blockingQueue, new MonitorMessageInfo(Constants.MONITOR_AK_STATUS_ACTION, secretName, null, String.format("Secret[%s] ak expire and fail to refresh", secretName), true));
                            }
                        }
                    }
                } else {
                    CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).errorf("Secret[{}] ExtendedConfig is invalid", secretName);
                }
            }
        }
    }

    private boolean judgeSecretExpired(String rotationInterval, long refreshTimestamp) {
        if (StringUtils.isEmpty(rotationInterval)) {
            return false;
        }
        int interval = Integer.parseInt(rotationInterval.replace("s", ""));
        if (interval < 0) {
            throw new IllegalArgumentException("RotationInterval is invalid");
        }
        return System.currentTimeMillis() > refreshTimestamp + interval * 1000;
    }
}
