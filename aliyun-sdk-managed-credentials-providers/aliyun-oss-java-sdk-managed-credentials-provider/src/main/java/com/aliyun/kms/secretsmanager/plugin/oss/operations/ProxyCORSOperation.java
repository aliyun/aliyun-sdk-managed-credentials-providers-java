package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPlugin;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.RequestHandler;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.internal.CORSOperation;

import java.util.List;

public class ProxyCORSOperation extends CORSOperation implements ProxyOSSOperation {
    private final String secretName;
    private final AKExpireHandler akExpireHandler;
    private final AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    public ProxyCORSOperation(ServiceClient client, CredentialsProvider credsProvider, String secretName, AKExpireHandler akExpireHandler, AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        super(client, credsProvider);
        this.secretName = secretName;
        this.akExpireHandler = akExpireHandler;
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    @Override
    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key, boolean keepResponseOpen, List<RequestHandler> requestHandlers, List<ResponseHandler> reponseHandlers) throws OSSException, ClientException {
        try {
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, reponseHandlers);
        } catch (OSSException e) {
            checkAndRefreshSecretInfo(e, secretName, akExpireHandler, secretsManagerPlugin);
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, reponseHandlers);
        }
    }
}