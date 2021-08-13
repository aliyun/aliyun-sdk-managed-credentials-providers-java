package com.aliyun.kms.secretsmanager.plugin.common;

public abstract class DefaultAKExpireHandler<TException> implements AKExpireHandler<TException> {

    @Override
    public boolean judgeAKExpire(TException e) {
        if (getAKExpireCode().equals(getErrorCode(e))) {
            return true;
        }
        return false;
    }

    abstract public String getErrorCode(TException e);

    abstract public String getAKExpireCode();
}
