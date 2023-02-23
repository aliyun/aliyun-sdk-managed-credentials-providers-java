package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.RequestHandler;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.internal.OSSBucketOperation;

import java.io.InputStream;
import java.util.List;

public class ProxyOSSBucketOperation extends OSSBucketOperation implements ProxyOSSOperation {
    private final String secretName;
    private final AKExpireHandler akExpireHandler;

    public ProxyOSSBucketOperation(ServiceClient client, OSSClient ossClient, String secretName, AKExpireHandler akExpireHandler) {
        super(client, ossClient.getCredentialsProvider());
        this.secretName = secretName;
        this.akExpireHandler = akExpireHandler;
    }

    @Override
    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key, boolean keepResponseOpen, List<RequestHandler> requestHandlers, List<ResponseHandler> reponseHandlers) throws OSSException, ClientException {
        InputStream content = request.getContent();
        long contentLength = request.getContentLength();
        try {
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, reponseHandlers);
        } catch (OSSException e) {
            checkAndRefreshSecretInfo(e, secretName, akExpireHandler, secretsManagerPlugin);
            request.setContent(content);
            request.setContentLength(contentLength);
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, reponseHandlers);
        }
    }
}