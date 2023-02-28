#  Aliyun SDK Managed Credentials Providers for Java
The Aliyun SDK Managed Credentials Providers for Java enables Java developers to easily access to other Aliyun Services using managed RAM credentials stored in Aliyun Secrets Manager. You can get started in minutes using Maven .

Read this in other languages: [English](README.md), [简体中文](README.zh-cn.md)

- [Aliyun Secrets Manager Managed RAM Credentials Summary](https://www.alibabacloud.com/help/doc-detail/152001.htm)
- [Issues](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/issues)
- [Release](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/releases)

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)


## Features

* Provide an easy method to other AliCould Services using managed RAM credentials
* Provides multiple access ways such as ECS instance RAM Role or Client Key to obtain a managed RAM credentials
* Provides the Aliyun Service client to refresh the RAM credentials automatically


## Requirements

- Your secret must be a managed RAM credentials
- Java 1.8 or later
- Maven

## Background
When applications use Aliyun SDK to call Alibaba Cloud Open APIs, access keys are traditionally used to authenticate to the cloud service. While access keys are easy to use they present security risks that could be leveraged by adversarial developers or external threats.

Alibaba Cloud SecretsManager is a solution that helps mitigate the risks by allowing organizations centrally manage access keys for all applications, allowing automatically or mannually rotating them without interrupting the applications. The managed access keys in SecretsManager is called [Managed RAM Credentials](https://www.alibabacloud.com/help/doc-detail/212421.htm).

For more advantages of using SecretsManager, refer to [SecretsManager Overview](https://www.alibabacloud.com/help/doc-detail/152001.htm).

## Client Mechanism
Applications use the access key that is managed by SecretsManager via the 'Secret Name' representing the access key.

The Managed Credentials Provider periodically obtains the Access Key represented by the secret name and supply it to Aliyun SDK when accessing Alibaba Cloud Open APIs. The provider normally refreshes the locally cached access key at a specified interval, which is configurable.

However, there are circumstances that the cached access key is no longer valid, which typically happens when emergent access key rotation is performed by adminstrators in SecretsManager to respond to an leakage incident. Using invalid access key to call Open APIs usually results in an exception that corresponds to an API error code. The Managed Credentials Provider will immediately refresh the cached access key and retry the failed Open API if the corresponding error code is `InvalidAccessKeyId.NotFound` or `InvalidAccessKeyId`. 

Application developers can override or extend this behavior for specific cloud services if the APIs return other error codes for using expired access keys. Refer to [Modifying the default expire handler](#modifying-the-default-expire-handler).

## Install

The recommended way to use the Aliyun SDK Managed Credentials Providers for Java in your project is to consume it from Maven. Take an example to import OSS credentials provider maven:

```
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-sdk-oss-managed-credentials-provider</artifactId>
    <version>1.2.1</version>
</dependency>
```


## Build

Once you check out the code from GitHub, you can build it using Maven. Use the following command to build:

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## Support Aliyun Services

The Aliyun SDK Managed Credentials Providers for Java supports the following Aliyun Services:

| Aliyun SDK Name | SDK maven(groupId:artifactId) | Supported Versions | Plugin Name | Import plugin maven(groupId:artifactId)  | 
| :----:| :----: | :----: |  :----: | :----: |
| Alibaba Cloud SDK | com.aliyun:aliyun-java-sdk-core | 4.3.2~4.5.17 | [Managed Credentials Provider for Aliyun Java SDK](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/aliyun-java-sdk-managed-credentials-provider) |  com.aliyun:aliyun-java-sdk-core-managed-credentials-provider  | 
| OSS Java SDK | com.aliyun.oss:aliyun-sdk-oss | 2.1.0~3.10.2 | [Managed Credentials Provider for Aliyun Java OSS SDK](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/aliyun-oss-java-sdk-managed-credentials-provider) | com.aliyun:aliyun-sdk-oss-managed-credentials-provider  | 
| ONS | com.aliyun.openservices:ons-client | 1.8.5.Final~1.8.7.4.Final |[Managed Credentials Provider for ONS Java Client](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/ons-client-managed-credentials-provider) | com.aliyun:ons-client-managed-credentials-provider  | 

## Aliyun Managed Credentials Providers Sample 

### Step 1: Configure the credentials provider

Use configuration file(`managed_credentials_providers.properties`)to access KMS([Configuration file setting for details](../../README_config.md))，You could use the recommended way to access KMS with Client Key.

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

You cloud use the following code to access Aliyun services with managed RAM credentials(taking an example to access oss).

```Java
import com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;

import java.util.List;
    
public class OssPluginSample {

    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";

        // must use the follow method to in the client
        OSS ossClient = new ProxyOSSClientBuilder().build(endpoint, secretName);

        List<Bucket> buckets = ossClient.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket != null) {
                // do something with bucket
            }
        }

        // must use the follow method to close the client for releasing provider resource
         ossClient.shutdown();
    }
}
```

### Step 2 (Alternative): Integrate with Spring Bean 

You cloud use the following way to integrate Aliyun SDK with Spring Bean(taking an example of Oss SDK).

```XML
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
              http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean name="proxyOSSClientBuilder" class="com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder" scope="singleton" />
</beans>

```

## Modifying the default expire handler

With Aliyun SDK Managed Credentials Provider that supports customed error retry, you can customize the error retry judgment of the client due to manual rotation of credentials in extreme scenarios, you only implement the following interface.

  ```Java
package com.aliyun.kms.secretsmanager.plugin.common;

public interface AKExpireHandler<TException> {

    /**
     * judge whether the exception is caused by AccessKey expiration
     *
     * @param e
     * @return
     */
    boolean judgeAKExpire(TException e);
}

  ```

The sample codes below show customed judgment exception interface and use it to call aliyun services.


  ```Java

import com.aliyun.kms.secretsmanager.plugin.sdkcore.ProxyAcsClient;
import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;

import java.util.HashSet;
import java.util.Set;

public class SdkRetrySample {
    
    public static void main(String[]args) throws Exception{
        String region="cn-hangzhou";
        String secretName="******";
        
        // get an ACSClient
        // provide the given error codes to obtain the credentials again
        IAcsClient client = new ProxyAcsClient(region, secretName, new AliyunSdkAKExpireHandler(new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId"}));

        // business code: your code that calls Cloud Open API
        invoke(client,region);

        // must use the follow method to close the client for releasing provider resource
        client.shutdown();
    }
}

class AliyunSdkAKExpireHandler implements AKExpireHandler<ClientException> {

    private final static String[] AK_EXPIRE_ERROR_CODES = new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId"};

    private Set<String> errorCodeSet = new HashSet<>();

    public AliyunSdkAKExpireHandler() {
        for (String akExpireCode : AK_EXPIRE_ERROR_CODES) {
            errorCodeSet.add(akExpireCode);
        }
    }

    public AliyunSdkAKExpireHandler(String[] akExpireErrorCodes) {
        if (akExpireErrorCodes == null || akExpireErrorCodes.length == 0) {
            for (String akExpireCode : AK_EXPIRE_ERROR_CODES) {
                errorCodeSet.add(akExpireCode);
            }
        } else {
            for (String akExpireCode : akExpireErrorCodes) {
                errorCodeSet.add(akExpireCode);
            }
        }
    }

    @Override
    public boolean judgeAKExpire(ClientException e) {
        return errorCodeSet.contains(e.getErrCode());
    }

}

  ```
