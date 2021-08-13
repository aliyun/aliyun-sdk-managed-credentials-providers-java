package com.aliyun.kms.secretsmanager.plugin.common.service;

import com.aliyun.kms.secretsmanager.plugin.common.utils.Constants;
import com.aliyun.kms.secretsmanager.plugin.common.utils.DateUtils;
import com.aliyun.kms.secretsmanager.plugin.common.utils.StringUtils;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.CacheSecretInfo;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.service.RefreshSecretStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class RotateAKSecretRefreshSecretStrategy implements RefreshSecretStrategy {

    /**
     * default rotation interval
     */
    private static final long DEFAULT_ROTATION_INTERVAL = 6 * 60 * 60 * 1000L;

    /**
     * default delay rotation interval
     */
    private static final long DEFAULT_DELAY_INTERVAL = 5 * 60 * 1000L;

    /**
     * default random disturbance range
     */
    private static final int DEFAULT_RANDOM_DISTURBANCE_RANGE = 5 * 60 * 1000;

    /**
     * the gson object
     */
    private static final Gson gson = new Gson();

    private long rotationInterval;
    private long delayInterval;
    private long randomDisturbance;

    public RotateAKSecretRefreshSecretStrategy() {
        this(DEFAULT_ROTATION_INTERVAL, DEFAULT_DELAY_INTERVAL);
    }

    public RotateAKSecretRefreshSecretStrategy(long rotationInterval, long delayInterval) {
        if (rotationInterval > 0) {
            this.rotationInterval = rotationInterval;
        } else {
            this.rotationInterval = DEFAULT_ROTATION_INTERVAL;
        }
        if (delayInterval > 0) {
            this.delayInterval = delayInterval;
        } else {
            this.delayInterval = DEFAULT_DELAY_INTERVAL;
        }
        this.randomDisturbance = new Random().nextInt(DEFAULT_RANDOM_DISTURBANCE_RANGE);
    }

    @Override
    public void init() throws CacheSecretException {
        // do nothing
    }

    @Override
    public long getNextExecuteTime(String secretName, long ttl, long offsetTimestamp) {
        long now = System.currentTimeMillis();
        if (ttl + offsetTimestamp > now) {
            return ttl + offsetTimestamp + randomDisturbance;
        } else {
            return now + ttl + randomDisturbance;
        }
    }

    @Override
    public long parseNextExecuteTime(CacheSecretInfo cacheSecretInfo) {
        SecretInfo secretInfo = cacheSecretInfo.getSecretInfo();
        long nextRotationDate = parseNextRotationDate(secretInfo);
        long now = System.currentTimeMillis();
        if (nextRotationDate >= now + rotationInterval + randomDisturbance || nextRotationDate <= now) {
            return now + rotationInterval + randomDisturbance;
        } else {
            return nextRotationDate + delayInterval + randomDisturbance;
        }
    }

    @Override
    public long parseTTL(SecretInfo secretInfo) {
        if (!StringUtils.isEmpty(secretInfo.getSecretType()) && Constants.RAM_CREDENTIALS_SECRET_TYPE.equalsIgnoreCase(secretInfo.getSecretType())) {
            String extendedConfig = secretInfo.getExtendedConfig();
            if (extendedConfig != null) {
                Map<String, String> map = gson.fromJson(extendedConfig, Map.class);
                String secretSubType = map.getOrDefault(Constants.EXTENDED_CONFIG_PROPERTY_SECRET_SUB_TYPE, "");
                if (Constants.RAM_USER_ACCESS_KEY_SECRET_SUB_TYPE.equals(secretSubType)) {
                    String rotationInterval = secretInfo.getRotationInterval();
                    if (StringUtils.isEmpty(rotationInterval)) {
                        return this.rotationInterval + randomDisturbance;
                    }
                    return Long.parseLong(rotationInterval.replace("s", "")) * 1000 + randomDisturbance;
                }
            }
        }
        JsonObject json = gson.fromJson(secretInfo.getSecretValue(), JsonObject.class);
        if (json.get(Constants.PROPERTY_NAME_KEY_REFRESH_INTERVAL) == null) {
            return -1;
        }
        return json.getAsJsonPrimitive(Constants.PROPERTY_NAME_KEY_REFRESH_INTERVAL).getAsLong() * 1000 + randomDisturbance;


    }

    private long parseNextRotationDate(SecretInfo secretInfo) {
        String nextRotationDate = secretInfo.getNextRotationDate();
        if (StringUtils.isEmpty(nextRotationDate)) {
            JsonObject json = new Gson().fromJson(secretInfo.getSecretValue(), JsonObject.class);
            if (json.get(Constants.PROPERTY_NAME_KEY_SCHEDULE_ROTATE_TIMESTAMP) == null) {
                return -1;
            }
            return json.getAsJsonPrimitive(Constants.PROPERTY_NAME_KEY_SCHEDULE_ROTATE_TIMESTAMP).getAsLong() * 1000;
        }
        return DateUtils.parseDate(nextRotationDate, DateUtils.TIMEZONE_DATE_PATTERN);
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
