package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.oss.internal.OSSUploadOperation;

public class ProxyOSSUploadOperation extends OSSUploadOperation implements ProxyOSSOperation {

    public ProxyOSSUploadOperation(ProxyOSSMultipartOperation objectOperation) {
        super(objectOperation);
    }
}