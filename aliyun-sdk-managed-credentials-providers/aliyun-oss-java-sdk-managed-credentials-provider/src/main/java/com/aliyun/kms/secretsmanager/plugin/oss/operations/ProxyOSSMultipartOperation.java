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
import com.aliyun.oss.common.comm.io.BoundedInputStream;
import com.aliyun.oss.common.comm.io.RepeatableBoundedFileInputStream;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.internal.OSSMultipartOperation;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.WebServiceRequest;
import com.aliyuncs.kms.secretsmanager.client.utils.CacheClientConstant;
import com.aliyuncs.kms.secretsmanager.client.utils.CommonLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.List;

public class ProxyOSSMultipartOperation extends OSSMultipartOperation implements ProxyOSSOperation {

    private final String secretName;
    private final AKExpireHandler akExpireHandler;

    public ProxyOSSMultipartOperation(ServiceClient client, OSSClient ossClient, String secretName, AKExpireHandler akExpireHandler) {
        super(client, ossClient.getCredentialsProvider());
        this.secretName = secretName;
        this.akExpireHandler = akExpireHandler;
    }

    @Override
    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key,
                                boolean keepResponseOpen, List<RequestHandler> requestHandlers,
                                List<ResponseHandler> responseHandlers) throws OSSException, ClientException {
        InputStream originalContent = request.getContent();
        long contentLength = request.getContentLength();
        long partStartPosition = extractPartStartPosition(request);

        try {
            return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, responseHandlers);
        } catch (OSSException e) {
            checkAndRefreshSecretInfo(e, secretName, akExpireHandler, secretsManagerPlugin);
            return retryOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers,
                    responseHandlers, originalContent, contentLength, partStartPosition);
        }
    }

    private <T> T retryOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key,
                                 boolean keepResponseOpen, List<RequestHandler> requestHandlers,
                                 List<ResponseHandler> responseHandlers,
                                 InputStream originalContent, long contentLength, long partStartPosition) throws OSSException, ClientException {
        InputStream newContent = rebuildInputStream(request, originalContent, partStartPosition);

        if (newContent != null) {
            request.setContent(newContent);
        } else {
            OssPluginUtils.resetInputStream(originalContent);
            request.setContent(originalContent);
        }
        request.setContentLength(contentLength);

        return super.doOperation(request, parser, bucketName, key, keepResponseOpen, requestHandlers, responseHandlers);
    }

    private InputStream rebuildInputStream(RequestMessage request, InputStream originalContent, long partStartPosition) {
        WebServiceRequest originalRequest = request.getOriginalRequest();
        if (!(originalRequest instanceof UploadPartRequest)) {
            return null;
        }

        UploadPartRequest uploadPartRequest = (UploadPartRequest) originalRequest;
        InputStream userStream = uploadPartRequest.getInputStream();

        if (!(userStream instanceof FileInputStream)) {
            return null;
        }

        try {
            FileInputStream newStream = rebuildFileInputStream((FileInputStream) userStream, partStartPosition);
            if (newStream != null) {
                uploadPartRequest.setInputStream(newStream);
                return IOUtils.newRepeatableInputStream(uploadPartRequest.buildPartialStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to rebuild input stream for retry", e);
        }

        return null;
    }

    private long extractPartStartPosition(RequestMessage request) {
        WebServiceRequest originalRequest = request.getOriginalRequest();
        if (!(originalRequest instanceof UploadPartRequest)) {
            return 0;
        }

        InputStream userStream = ((UploadPartRequest) originalRequest).getInputStream();
        if (!(userStream instanceof FileInputStream)) {
            return 0;
        }

        InputStream content = request.getContent();
        if (!(content instanceof RepeatableBoundedFileInputStream)) {
            return 0;
        }

        InputStream wrapped = ((RepeatableBoundedFileInputStream) content).getWrappedInputStream();
        if (!(wrapped instanceof BoundedInputStream)) {
            return 0;
        }

        try {
            Field posField = BoundedInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            long pos = (long) posField.get(wrapped);

            FileInputStream fis = (FileInputStream) ((BoundedInputStream) wrapped).getWrappedInputStream();
            FileChannel channel = fis.getChannel();
            long currentPos = channel.position();

            return currentPos - pos;
        } catch (Exception e) {
            CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).warnf("Failed to extract part start position", e);
            return 0;
        }
    }

    private FileInputStream rebuildFileInputStream(FileInputStream originalStream, long skipBytes) {
        try {
            Field pathField = FileInputStream.class.getDeclaredField(OssPluginUtils.PATH_FIELD_NAME);
            pathField.setAccessible(true);
            String path = (String) pathField.get(originalStream);

            FileInputStream newStream = new FileInputStream(path);
            if (skipBytes > 0) {
                newStream.skip(skipBytes);
            }
            return newStream;
        } catch (Exception e) {
            CommonLogger.getCommonLogger(CacheClientConstant.MODE_NAME).warnf("Failed to rebuild FileInputStream", e);
            return null;
        }
    }

}