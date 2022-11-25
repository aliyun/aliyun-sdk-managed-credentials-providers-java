import com.aliyun.kms.secretsmanager.plugin.sdkcore.SecretsManagerSdkCorePluginManager;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;
import com.aliyuncs.IAcsClient;
import org.junit.Test;

public class SecretsManagerSdkCorePluginManagerTest {

    @Test
    public void testGetSdkCoreManager(){
        try {
            AliyunSDKSecretsManagerPluginsManager.init();
            IAcsClient acsClient = SecretsManagerSdkCorePluginManager.getSdkCoreClient("cn-hangzhou", "SecurityClientTest");
            System.out.println(acsClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    @Test
//    public void testPlugin() throws ClientException {
//        DefaultAcsClient client = new ProxyAcsClient("<regionId>","<secretName>");
//        DescribeInstancesRequest req = new DescribeInstancesRequest();
//        DescribeInstancesResponse response = client.getAcsResponse(req);
//        System.out.println(response.getInstances());
//        client.shutdown();
//    }
}
