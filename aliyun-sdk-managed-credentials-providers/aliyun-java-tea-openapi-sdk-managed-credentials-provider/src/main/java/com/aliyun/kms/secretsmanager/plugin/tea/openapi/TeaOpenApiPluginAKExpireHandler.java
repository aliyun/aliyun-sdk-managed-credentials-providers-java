package com.aliyun.kms.secretsmanager.plugin.tea.openapi;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.tea.TeaException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class TeaOpenApiPluginAKExpireHandler implements AKExpireHandler<TeaException> {

    private final static String[] AK_EXPIRE_ERROR_CODES = new String[]{"InvalidAccessKeyId.NotFound","InvalidAccessKeyId","Unauthorized"};

    private final Set<String> errorCodeSet = new HashSet<>();

    public TeaOpenApiPluginAKExpireHandler() {
        Collections.addAll(errorCodeSet, AK_EXPIRE_ERROR_CODES);
    }

    public TeaOpenApiPluginAKExpireHandler(String[] akExpireErrorCodes) {
        if (akExpireErrorCodes == null || akExpireErrorCodes.length == 0) {
            errorCodeSet.addAll(Arrays.asList(AK_EXPIRE_ERROR_CODES));
        } else {
            errorCodeSet.addAll(Arrays.asList(akExpireErrorCodes));
        }
    }

    @Override
    public boolean judgeAKExpire(TeaException e) {
        for (String errorCode : errorCodeSet) {
            if (errorCode.equals(e.getCode())) {
                return true;
            }
        }
        return false;
    }

}
