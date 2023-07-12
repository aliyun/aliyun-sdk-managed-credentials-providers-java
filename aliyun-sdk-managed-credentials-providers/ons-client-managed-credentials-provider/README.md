# Managed Credentials Provider for ONS Java Client 

The Managed Credentials Provider for ONS Java Client enables Java developers to easily access to Aliyun ONS Services using managed RAM credentials stored in Aliyun Secrets Manager. You can get started in minutes using Maven .

Read this in other languages: [English](README.md), [简体中文](README.zh-cn.md)

## Background
When applications use Aliyun ONS Client to call Alibaba Cloud Open Notification Service, access keys are traditionally used to authenticate to the cloud service. While access keys are easy to use they present security risks that could be leveraged by adversarial developers or external threats.

Alibaba Cloud SecretsManager is a solution that helps mitigate the risks by allowing organizations centrally manage access keys for all applications, allowing automatically or mannually rotating them without interrupting the applications. The managed access keys in SecretsManager is called [Managed RAM Credentials](https://www.alibabacloud.com/help/doc-detail/212421.htm).

For more advantages of using SecretsManager, refer to [SecretsManager Overview](https://www.alibabacloud.com/help/doc-detail/152001.htm).

## Client Mechanism
Applications use the access key that is managed by SecretsManager via the `Secret Name` representing the access key.

The Managed Credentials Provider periodically obtains the Access Key represented by the secret name and supply it to Aliyun SDK when accessing Alibaba Cloud Open Notification Service. The provider normally refreshes the locally cached access key at a specified interval, which is configurable.

However, there are circumstances that the cached access key is no longer valid, which typically happens when emergent access key rotation is performed by adminstrators in SecretsManager to respond to a leakage incident. Using invalid access key to call Open Notification Service usually results in an exception that corresponds to an API error code. If the corresponding access key cannot be used, the user needs to force to refresh the cached access key immediately, then retry to call the fail API. Refer to [Expiration refresh handler](#expiration-refresh-handler).


## Install

The recommended way to use the Managed Credentials Provider for ONS Java Client in your project is to consume it from Maven. Import it as follows:

```XML

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>4.5.17</version>
</dependency>
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>ons-client</artifactId>
    <version>[1.8.5.Final,1.8.7.3.Final]</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>ons-client-managed-credentials-provider</artifactId>
    <version>1.3.1</version>
</dependency>

```

## Build

Once you check out the code from GitHub, you can build it using Maven. Use the following command to build:

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## ONS Client Managed Credentials Provider Sample

### Step 1: Configure the credentials provider

Use configuration file(`managed_credentials_providers.properties`)to access
KMS([Configuration file setting for details](../../README_config.md))，You could use the recommended way to access KMS with Client Key.

```properties
## the type of access credentials
credentials_type=client_key

## you could read the password of client key from environment variable or file
client_key_password_from_env_variable=#your client key private key password environment variable name#
client_key_password_from_file_path=#your client key private key password file path#

## the private key file path of the Client Key
client_key_private_key_path=#your client key private key file path#

## the region related to the kms service
cache_client_region_id=[{"regionId":"#regionId#"}]
```

### Step 2: Use the credentials provider in Aliyun SDK

You cloud use the following code to access ONS services with managed RAM credentials.

```Java
import com.aliyun.kms.secretsmanager.plugin.ons.ProxyOnsProducerBuilder;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;

import java.util.Properties;

public class OnsProviderSample {
    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "xxxxxxxxxxxxx";
        String groupId = "xxxxxxxx";
        String sendMsgTimeoutMillis = "xxxxxx";
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, groupId);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, endpoint);
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, sendMsgTimeoutMillis);
        //custom config name
        //ConfigLoader.setConfigName("your-config-name");
        // get an ons producer
        Producer producer = new ProxyOnsProducerBuilder().build(properties, secretName);
        producer.start();
        
        // business code: your code that calls Cloud Open Notification Service

        // must use the follow method to close the client for releasing provider resource
        producer.shutdown();
    }
}
```

### Step 2 (Alternative): Integrate with Spring Bean

You cloud use the following way to integrate it with Spring Bean.

```XML
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
              http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean name="proxyOnsConsumerBuilder" class="com.aliyun.kms.secretsmanager.plugin.ons.ProxyOnsConsumerBuilder" />
</beans>

```
## Expiration refresh handler

When the corresponding access key cannot be used, the user needs to force to refresh the cached access key immediately, then retry to call the fail API. Please refer to the following code.

```Java
import com.aliyun.kms.secretsmanager.plugin.ons.ProxyOnsProducerBuilder;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.exception.ONSClientException;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.kms.secretsmanager.plugin.common.AliyunSDKSecretsManagerPluginsManager;

import java.util.Properties;

public class OnsProviderRetrySample {
    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "xxxxxxxxxxxxx";
        String groupId = "xxxxxxxx";
        String sendMsgTimeoutMillis = "xxxxxx";
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, groupId);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, endpoint);
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, sendMsgTimeoutMillis);
        //custom config name
        //ConfigLoader.setConfigName("your-config-name");
        // get Ons producer
        Producer producer = new ProxyOnsProducerBuilder().build(properties, secretName);
        producer.start();

        // business code：your code that calls Cloud Open Notification Service
        Message msg = new Message("producer_retry", "retry", "Retry MQ producer".getBytes());

        try {
            SendResult sendResult = producer.send(msg);
        } catch (ONSClientException e) {
            System.out.println("send fail");
            if (e.getMessage() != null && e.getMessage().contains("signature validate failed")) {
                AliyunSDKSecretsManagerPluginsManager.refreshSecretInfo(secretName);
                SendResult sendResult = producer.send(msg);
            }
        }

        // must use the follow method to close the client for releasing provider resource
        producer.shutdown();
    }
}
```
