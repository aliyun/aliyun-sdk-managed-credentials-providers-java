package com.aliyun.kms.secretsmanager.plugin.oss;

import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;

import java.util.List;
import java.util.stream.Collectors;

public class OSSClientSample {

    static {
        try {
            AliyunSDKSecretsManagerPluginsManager.init();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    AliyunSDKSecretsManagerPluginsManager.shutdown();
                } catch (Exception ignore) {
                }
            }));
        } catch (CacheSecretException e) {
            e.printStackTrace();
        }
    }

    private String secretName;
    private OSS client;

    public static void main(String[] args) {
        OSSClientSample ossClient = new OSSClientSample("https://oss-cn-hangzhou.aliyuncs.com", "sdk-core-secret");
        OSS client = ossClient.getClient();
        for (int index = 0; index > 10; index++) {
            try {
                List<Bucket> bucketList = client.listBuckets();
                System.out.println("bucket list:" + bucketList.stream().map(o -> o.getName()).collect(Collectors.joining(",")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (Exception ignore) {
            }
        }
        ossClient.shutdown();
    }

    public OSSClientSample(String regionId, String secretName) {
        this.secretName = secretName;
        try {
            client = SecretsManagerOssPluginManager.getOssClient(regionId, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }

    public OSS getClient() {
        return client;
    }

    public void shutdown() {
        if (this.client != null) {
            try {
                SecretsManagerOssPluginManager.closeOssClient(client, secretName);
            } catch (Exception ignore) {
            }
        }
    }
}
