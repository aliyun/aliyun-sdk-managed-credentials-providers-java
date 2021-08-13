package com.aliyun.kms.secretsmanager.plugin.common.utils;

import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.google.gson.Gson;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import static com.aliyun.kms.secretsmanager.plugin.common.utils.Constants.*;

public class CredentialsUtils {

    private static final Gson gson = new Gson();

    private CredentialsUtils() {

    }

    public static SecretsManagerPluginCredentials generateCredentialsBySecret(String secretData) {
        if (!StringUtils.isEmpty(secretData)) {
            Properties accessKeyInfo = new Gson().fromJson(secretData, Properties.class);
            String accessKeyId = null;
            String accessKeySecret = null;
            if (accessKeyInfo.containsKey(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_ID) && accessKeyInfo.containsKey(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_SECRET)) {
                accessKeyId = accessKeyInfo.getProperty(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_ID);
                accessKeySecret = accessKeyInfo.getProperty(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_SECRET);
            } else {
                throw new IllegalArgumentException(String.format("illegal secret data[%s]", secretData));
            }
            long expireTimestamp = Constants.NOT_SUPPORT_TAMP_AK_TIMESTAMP;
            if (accessKeyInfo.containsKey(Constants.PROPERTY_NAME_KEY_EXPIRE_TIMESTAMP)) {
                expireTimestamp = DateUtils.parseDate(accessKeyInfo.getProperty(Constants.PROPERTY_NAME_KEY_EXPIRE_TIMESTAMP), DateUtils.TIMEZONE_DATE_PATTERN);
            }
            return new SecretsManagerPluginCredentials(accessKeyId, accessKeySecret, expireTimestamp);
        } else {
            throw new IllegalArgumentException("Missing param secretData");
        }
    }

    public static SecretInfo generateSecretInfoByCredentials(SecretsManagerPluginCredentials secretsManagerPluginCredentials, String secretName) {
        if (secretsManagerPluginCredentials != null) {
            Properties accessKeyInfo = new Properties();
            accessKeyInfo.put(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_ID, secretsManagerPluginCredentials.getAccessKeyId());
            accessKeyInfo.put(Constants.PROPERTY_NAME_KEY_ACCESS_KEY_SECRET, secretsManagerPluginCredentials.getAccessKeySecret());
            String secretValue = gson.toJson(accessKeyInfo);
            String versionId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            String createTime = DateUtils.formatDate(new Date(now), DateUtils.TIMEZONE_DATE_PATTERN);
            String nextRotationDate = DateUtils.formatDate(new Date(now + 24 * 60 * 60 * 1000), DateUtils.TIMEZONE_DATE_PATTERN);
            return new SecretInfo(secretName, versionId, secretValue, CacheClientConstant.TEXT_DATA_TYPE, createTime, RAM_CREDENTIALS_SECRET_TYPE, DEFAULT_AUTOMATIC_ROTATION, "", DEFAULT_ROTATION_INTERVAL, nextRotationDate);
        } else {
            throw new IllegalArgumentException("Missing param secretsManagerPluginCredentials");
        }
    }
}
