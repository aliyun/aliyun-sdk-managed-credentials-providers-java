package com.aliyun.kms.secretsmanager.plugin.oss.operations;

import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.oss.utils.OssPluginUtils;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.comm.RequestHandler;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.comm.io.RepeatableFileInputStream;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.internal.OSSObjectOperation;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;


public class ProxyOSSObjectOperation extends OSSObjectOperation implements ProxyOSSOperation {
    private final String secretName;
    private final AKExpireHandler akExpireHandler;

    public ProxyOSSObjectOperation(ServiceClient client, OSSClient ossClient, String secretName, AKExpireHandler akExpireHandler) {
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
            InputStream newStream = rebuildFromFile(request);
            if (newStream == null) {
                newStream = rebuildFromInputStream(content);
            }
            if (newStream != null) {
                request.setContent(newStream);
            } else {
                OssPluginUtils.resetInputStream(content);
                request.setContent(content);
            }
            request.setContentLength(contentLength);
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, reponseHandlers);
        }
    }

    private InputStream rebuildFromFile(RequestMessage request) {
        Object originalReq = request.getOriginalRequest();
        if (originalReq instanceof PutObjectRequest) {
            File file = ((PutObjectRequest) originalReq).getFile();
            if (file != null) {
                try {
                    return new RepeatableFileInputStream(file);
                } catch (IOException ex) {
                    throw new ClientException("Cannot locate file to upload: ", ex);
                }
            }
        }
        return null;
    }


    public static @Nullable RepeatableFileInputStream rebuildFromInputStream(InputStream originalStream) {
        if (originalStream instanceof RepeatableFileInputStream) {
            try {
                InputStream originalFileInputStream = ((RepeatableFileInputStream) originalStream).getWrappedInputStream();
                java.lang.reflect.Field pathField = FileInputStream.class.getDeclaredField(OssPluginUtils.PATH_FIELD_NAME);
                pathField.setAccessible(true);
                String path = (String) pathField.get(originalFileInputStream);
                if (path != null) {
                    return new RepeatableFileInputStream(new File(path));
                }
            } catch (Exception e) {
                CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).warnf("action:rebuildFromInputStream", e);
            }
        }
        return null;
    }
}