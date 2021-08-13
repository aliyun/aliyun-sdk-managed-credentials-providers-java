package com.aliyun.kms.secretsmanager.plugin.common.model;

import com.aliyun.kms.secretsmanager.plugin.common.utils.DateUtils;

import java.io.Serializable;
import java.util.Date;

public class MonitorMessageInfo implements Serializable {

    /**
     * the action while the monitor event occurs
     */
    private String action;

    private String secretName;

    private String accessKeyId;

    private String errorMessage;

    private boolean alarm;

    private String timestamp;

    public MonitorMessageInfo(String action, String secretName, String accessKeyId, String errorMessage) {
        this(action, secretName, accessKeyId, errorMessage, false);
    }

    public MonitorMessageInfo(String action, String secretName, String accessKeyId, String errorMessage, boolean alarm) {
        this.action = action;
        this.secretName = secretName;
        this.accessKeyId = accessKeyId;
        this.errorMessage = errorMessage;
        this.alarm = alarm;
        this.timestamp = DateUtils.formatDate(new Date(), DateUtils.TIMEZONE_DATE_PATTERN);
    }

    public String getAction() {
        return action;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean getAlarm() {
        return this.alarm;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}