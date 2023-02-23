package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.oss.internal.OSSDownloadOperation;

public class ProxyOSSDownloadOperation extends OSSDownloadOperation implements ProxyOSSOperation {
    public ProxyOSSDownloadOperation(ProxyOSSObjectOperation objectOperation) {
        super(objectOperation);
    }
}