# Aliyun Tea OpenAPI Java SDK Managed Credentials Plugin

Aliyun Tea OpenAPI Java SDK Managed Credentials Plugin helps Java developers more easily use RAM credentials managed in SecretsManager to access Aliyun service APIs. You can quickly use it via Maven. [View list of supported cloud products](https://github.com/aliyun/alibabacloud-java-sdk)

Read this in other languages: [English](README.md), [简体中文](README.zh-cn.md)

## Background
When applications use Aliyun Tea OpenAPI SDK to call Alibaba Cloud Open APIs, access keys are traditionally used to authenticate to the cloud service. While access keys are easy to use they present security risks that could be leveraged by adversarial developers or external threats.

Alibaba Cloud SecretsManager is a solution that helps mitigate the risks by allowing organizations centrally manage access keys for all applications, allowing automatically or mannually rotating them without interrupting the applications. The managed access keys in SecretsManager is called [Managed RAM Credentials](https://www.alibabacloud.com/help/doc-detail/212421.htm).

For more advantages of using SecretsManager, refer to [SecretsManager Overview](https://www.alibabacloud.com/help/doc-detail/152001.htm).

## Client Mechanism
Applications use the access key that is managed by SecretsManager via the `Secret Name` representing the access key.

The Managed Credentials Provider periodically obtains the Access Key represented by the secret name and supply it to Aliyun SDK when accessing Alibaba Cloud Open APIs. The provider normally refreshes the locally cached access key at a specified interval, which is configurable.

However, there are circumstances that the cached access key is no longer valid, which typically happens when emergent access key rotation is performed by adminstrators in SecretsManager to respond to a leakage incident. Using invalid access key to call Open APIs usually results in an exception that corresponds to an API error code. The Managed Credentials Provider will immediately refresh the cached access key and retry the failed Open API if the corresponding error code is `InvalidAccessKeyId.NotFound` ，`InvalidAccessKeyId` or `Unauthorized`. 

Application developers can override or extend this behavior for specific cloud services if the APIs return other error codes for using expired access keys. Refer to [Modifying the default expire handler](#modifying-the-default-expire-handler).

## Install

The recommended way to use the Managed Credentials Provider for Aliyun Java SDK in your project is to consume it from Maven. Import it as follows:

```XML
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-tea-openapi-sdk-managed-credentials-provider</artifactId>
        <version>1.3.5</version>
    </dependency>
```

## Build

Once you check out the code from GitHub, you can build it using Maven. Use the following command to build:

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## Aliyun SDK Managed Credentials Provider Sample(ECS as an example)

### Step 1: Install ECS SDK
```XML
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>ecs20140526</artifactId>
        <version>7.1.0</version>
    </dependency>
```

### Step 2: Configure the credentials provider

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

### Step 3: Use the credentials provider in Aliyun SDK

You could use the following code to access Aliyun services with managed RAM credentials。

```Java
import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeInstancesResponse;
import com.aliyun.kms.secretsmanager.plugin.tea.openapi.ProxyClientCreator;
import com.google.gson.Gson;

public class AliyunTeaOpenApiProviderSample {

    public static void main(String[] args) throws Exception {
        // Step 1: Specify the name of the managed credential (the name of the credential created in SecretsManager)
        String secretName = "your-secret-name";

        // Step 2: Configure the OpenAPI client
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.endpoint = "your-product-endpoint"; // Replace with the actual cloud product endpoint

        // Step 3: Create a client instance
        Client client = ProxyClientCreator.createClient(config, Client.class, secretName);

        // Step 4: Call the cloud service API
        com.aliyun.ecs20140526.models.DescribeInstancesRequest request = new com.aliyun.ecs20140526.models.DescribeInstancesRequest();
        request.setRegionId("cn-hangzhou"); // Set the region ID
        DescribeInstancesResponse response = client.describeInstances(request);

        // Step 5: Print the result
        System.out.println(new Gson().toJson(response.getBody()));
    }
}

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
import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeInstancesResponse;
import com.aliyun.kms.secretsmanager.plugin.common.AKExpireHandler;
import com.aliyun.kms.secretsmanager.plugin.tea.openapi.ProxyClientCreator;
import com.aliyun.tea.TeaException;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AliyunTeaOpenApiProviderSample {

    public static void main(String[] args) throws Exception {
        String secretName = "******";
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.endpoint = "#aliyun-product-endpoint#";
        Client client = ProxyClientCreator.createClient(config, Client.class, secretName, );
        com.aliyun.ecs20140526.models.DescribeInstancesRequest describeInstancesRequest = new com.aliyun.ecs20140526.models.DescribeInstancesRequest().setRegionId("cn-hangzhou");
        DescribeInstancesResponse describeInstancesResponse = client.describeInstances(describeInstancesRequest);
        System.out.println(new Gson().toJson(describeInstancesResponse.getBody()));
        // Step 1: Specify the name of the managed credential (the name of the credential created in SecretsManager)
        String secretName = "your-secret-name";

        // Step 2: Configure the OpenAPI client
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.endpoint = "your-product-endpoint"; // Replace with the actual cloud product endpoint

        // Step 3: Create an AK expiration handler
        TeaOpenApiAKExpireHandler akExpireHandler = new TeaOpenApiAKExpireHandler(new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId", "Unauthorized"});

        // Step 4: Create a client instance
        Client client = ProxyClientCreator.createClient(config, Client.class, secretName, akExpireHandler);

        // Step 5: Call the cloud service API
        com.aliyun.ecs20140526.models.DescribeInstancesRequest request = new com.aliyun.ecs20140526.models.DescribeInstancesRequest();
        request.setRegionId("cn-hangzhou"); // Set the region ID
        DescribeInstancesResponse response = client.describeInstances(request);

        // Step 6: Print the result
        System.out.println(new Gson().toJson(response.getBody()));
    }
}

class TeaOpenApiAKExpireHandler implements AKExpireHandler<TeaException> {

    private final static String[] AK_EXPIRE_ERROR_CODES = new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId", "Unauthorized"};

    private final Set<String> errorCodeSet = new HashSet<>();

    public TeaOpenApiAKExpireHandler() {
        Collections.addAll(errorCodeSet, AK_EXPIRE_ERROR_CODES);
    }

    public TeaOpenApiAKExpireHandler(String[] akExpireErrorCodes) {
        if (akExpireErrorCodes == null || akExpireErrorCodes.length == 0) {
            errorCodeSet.addAll(Arrays.asList(AK_EXPIRE_ERROR_CODES));
        } else {
            errorCodeSet.addAll(Arrays.asList(akExpireErrorCodes));
        }
    }

    @Override
    public boolean judgeAKExpire(TeaException e) {
        for (String errorCode : errorCodeSet) {
            if (errorCode.equals(e.getCode())) {
                return true;
            }
        }
        return false;
    }

}

  ```
