import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyun.kms.secretsmanager.plugin.sdkcore.ProxyAcsClient;
import com.aliyun.kms.secretsmanager.plugin.sdkcore.SecretsManagerSdkCorePluginManager;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.kms.model.v20160120.DescribeRegionsRequest;
import com.aliyuncs.kms.model.v20160120.DescribeRegionsResponse;
import com.aliyuncs.kms.model.v20160120.GetSecretValueRequest;
import com.aliyuncs.kms.model.v20160120.GetSecretValueResponse;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.profile.DefaultProfile;

import java.util.stream.Collectors;

public class SdkCoreClient {

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
    private IAcsClient client;

    public static void main(String[] args) throws Exception {
        IAcsClient client = new ProxyAcsClient("cn-hangzhou", "sdk-core-secret");
        for (int index = 0; index > 10; index++) {
            DescribeRegionsRequest request = new DescribeRegionsRequest();
            try {
                DescribeRegionsResponse response = client.getAcsResponse(request);
                System.out.println("region list:" + response.getRegions().stream().map(o -> o.getRegionId()).collect(Collectors.joining(",")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            GetSecretValueRequest getSecret = new GetSecretValueRequest();
            getSecret.setSecretName("common_secret");
            try {
                GetSecretValueResponse response = client.getAcsResponse(getSecret);
                System.out.println("secret value:" + response.getSecretData());
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (Exception ignore) {
            }
        }
        client.shutdown();
    }

    public SdkCoreClient(String regionId, String secretName) {
        this.secretName = secretName;
        try {
            DefaultProfile profile = DefaultProfile.getProfile("");
            client = SecretsManagerSdkCorePluginManager.getSdkCoreClient(profile, secretName);
        } catch (CacheSecretException e) {
            throw new RuntimeException(e);
        }
    }

    public IAcsClient getClient() {
        return client;
    }

    public void shutdown() {
        if (this.client != null) {
            try {
                SecretsManagerSdkCorePluginManager.closeSdkCoreClient(client, secretName);
            } catch (Exception ignore) {
            }
        }
    }
}