package com.aliyun.kms.secretsmanager.plugin.common;

public interface AKExpireHandler<TException> {

    /**
     * 判断异常是否由AK过期引起
     *
     * @param e
     * @return
     */
    boolean judgeAKExpire(TException e);
}
