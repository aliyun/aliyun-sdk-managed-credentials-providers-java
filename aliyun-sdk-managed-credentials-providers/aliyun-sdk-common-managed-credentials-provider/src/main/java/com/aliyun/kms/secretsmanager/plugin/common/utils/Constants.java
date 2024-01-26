package com.aliyun.kms.secretsmanager.plugin.common.utils;

public interface Constants {

    String PROPERTY_NAME_KEY_ACCESS_KEY_ID = "AccessKeyId";
    String PROPERTY_NAME_KEY_ACCESS_KEY_SECRET = "AccessKeySecret";
    String PROPERTY_NAME_KEY_EXPIRE_TIMESTAMP = "ExpireTimestamp";
    String KMS_SECRET_CURRENT_STAGE_VERSION = "ACSCurrent";
    /**
     * 客户端刷新周期，单位为s
     */
    String PROPERTY_NAME_KEY_REFRESH_INTERVAL = "RefreshInterval";
    /**
     * 轮转周期，单位为s
     */
    String PROPERTY_NAME_KEY_SCHEDULE_ROTATE_TIMESTAMP = "ScheduleRotateTimestamp";
    /**
     * 重试时间间隔，单位ms
     */
    Long RETRY_INITIAL_INTERVAL_MILLS = 2000L;

    /**
     * 最大等待时间，单位ms
     */
    Long CAPACITY = 10000L;
    /**
     * 重试最大尝试次数
     */
    Long RETRY_MAX_ATTEMPTS = 6 * 3600 * 1000 / CAPACITY;

    String LOGGER_NAME = "SecretsManagerPluginCommon";

    long NOT_SUPPORT_TAMP_AK_TIMESTAMP = -1;

    /**
     * 默认令牌数
     */
    int DEFAULT_MAX_TOKEN_NUMBER = 5;

    long DEFAULT_RATE_LIMIT_PERIOD = 10 * 60 * 1000L;

    String SECRETSMANAGER_PLUGIN_JAVA_OF_USER_AGENT = "aliyun-sdk-managed-credentials-providers-java";

    int SECRETSMANAGER_PLUGIN_JAVA_OF_USER_AGENT_PRIORITY = 1;

    String PROJECT_VERSION = "1.3.3";

    String PROPERTIES_MONITOR_PERIOD_MILLISECONDS_KEY = "monitor_period_milliseconds";

    String PROPERTIES_MONITOR_CUSTOMER_MILLISECONDS_KEY = "monitor_customer_milliseconds";

    String PROPERTIES_ROTATION_INTERVAL_KEY = "rotation_interval";

    String PROPERTIES_DELAY_INTERVAL_KEY = "delay_interval";

    String RAM_CREDENTIALS_SECRET_TYPE = "RamCredentials";

    String EXTENDED_CONFIG_PROPERTY_SECRET_SUB_TYPE = "SecretSubType";

    String RAM_USER_ACCESS_KEY_SECRET_SUB_TYPE = "RamUserAccessKey";

    String MONITOR_AK_STATUS_ACTION = "monitorAkStatus";

    String CUSTOM_MESSAGE_ACTION = "customMessage";

    String UPDATE_CREDENTIAL_ACTION = "updateCredential";

    String RECOVERY_GET_SECRET_ACTION = "recoveryGetSecret";

    String LOGGER_BASE_DIR_KEY = "base.dir";

    String DEFAULT_ROTATION_INTERVAL = "86400s";

    String DEFAULT_AUTOMATIC_ROTATION = "Enabled";

    String DEFAULT_CREDENTIAL_PROPERTIES_FILE_NAME = "managed_credentials_providers.properties";
}
