# Managed Credentials Provider for Aliyun Java OSS SDK

The Managed Credentials Provider for Aliyun Java OSS SDK enables Java developers to easily access to Aliyun OSS Services
using managed RAM credentials stored in Aliyun Secrets Manager. You can get started in minutes using Maven .

Read this in other languages: [English](README.md), [简体中文](README.zh-cn.md)

## Background

When applications use Aliyun SDK to call Alibaba Cloud Open APIs, access keys are traditionally used to authenticate to
the cloud service. While access keys are easy to use they present security risks that could be leveraged by adversarial
developers or external threats.

Alibaba Cloud SecretsManager is a solution that helps mitigate the risks by allowing organizations centrally manage
access keys for all applications, allowing automatically or mannually rotating them without interrupting the
applications. The managed access keys in SecretsManager is
called [Managed RAM Credentials](https://www.alibabacloud.com/help/doc-detail/212421.htm).

For more advantages of using SecretsManager, refer
to [SecretsManager Overview](https://www.alibabacloud.com/help/doc-detail/152001.htm).

## Client Mechanism

Applications use the access key that is managed by SecretsManager via the `Secret Name` representing the access key.

The Managed Credentials Provider periodically obtains the Access Key represented by the secret name and supply it to
Aliyun OSS SDK when accessing Alibaba Cloud OSS services. The provider normally refreshes the locally cached access key
at a specified interval, which is configurable.

However, there are circumstances that the cached access key is no longer valid, which typically happens when emergent
access key rotation is performed by adminstrators in SecretsManager to respond to a leakage incident. Using invalid
access key to call Open APIs usually results in an exception that corresponds to an API error code. The Managed
Credentials Provider will immediately refresh the cached access key and retry the failed Open API if the corresponding
error code is `InvalidAccessKeyId`.

Application developers can override or extend this behavior for specific cloud services if the APIs return other error
codes for using expired access keys. Refer
to [Modifying the default expire handler](#modifying-the-default-expire-handler).

## Install

The recommended way to use the Managed Credentials Provider for Aliyun Java OSS SDK in your project is to consume it
from Maven. Import it as follows:

```XML

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>4.5.17</version>
</dependency>
<dependency>
<groupId>com.aliyun.oss</groupId>
<artifactId>aliyun-sdk-oss</artifactId>
<version>[2.1.0,3.10.2]</version>
<exclusions>
    <exclusion>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-sdk-kms</artifactId>
    </exclusion>
</exclusions>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-sdk-oss-managed-credentials-provider</artifactId>
    <version>1.3.5</version>
</dependency>

```

## Build

Once you check out the code from GitHub, you can build it using Maven. Use the following command to build:

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## Aliyun OSS SDK Managed Credentials Provider Sample

### Step 1: Configure the credentials provider

Use configuration file(`managed_credentials_providers.properties`)to access
KMS([Configuration file setting for details](../../README_config.md))，You could use the recommended way to access KMS with
Client Key.

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

### Step 2: Use the credentials provider in Aliyun OSS SDK

You cloud use the following code to access OSS services with managed RAM credentials.

```Java
import com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;

import java.util.List;

public class OssProviderSample {

    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        //custom config name
        //ConfigLoader.setConfigName("your-config-name");
        // get Oss Client
        OSS ossClient = new ProxyOSSClientBuilder().build(endpoint, secretName);
        // business code: your code that calls OSS services
        List<Bucket> buckets = ossClient.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket != null) {
                // ...
            }
        }

        // must use the follow method to close the client for releasing provider resource
        ossClient.shutdown();
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
    <bean name="proxyOSSClientBuilder" class="com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder"
          scope="singleton"/>
</beans>

```

## Modifying the default expire handler

With Aliyun OSS SDK Managed Credentials Provider that supports customed error retry, you can customize the error retry
judgment of the client due to manual rotation of credentials in extreme scenarios, you only implement the following
interface.

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

The sample codes below show customed judgment exception interface and use it to call Aliyun OSS services.

  ```Java

import com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class OSSdkRetrySample {

    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        //custom config name
        //ConfigLoader.setConfigName("your-config-name");
        // get Oss Client
        OSS ossClient = new ProxyOSSClientBuilder().build(endpoint, secretName, new AliyunOSSSdkAKExpireHandler());
        // business code: your code that calls OSS services
        List<Bucket> buckets = ossClient.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket != null) {
                // ...
            }
        }

        // must use the follow method to close the client for releasing provider resource
        ossClient.shutdown();
    }
}

class AliyunOSSSdkAKExpireHandler implements AKExpireHandler<Exception> {

    private final static String[] AK_EXPIRE_ERROR_CODES = new String[]{"InvalidAccessKeyId"};


    private Set<String> errorCodeSet = new HashSet<>();

    public AliyunOSSSdkAKExpireHandler() {
        for (String akExpireCode : AK_EXPIRE_ERROR_CODES) {
            errorCodeSet.add(akExpireCode);
        }
    }

    public AliyunOSSSdkAKExpireHandler(String[] akExpireErrorCodes) {
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
    public boolean judgeAKExpire(Exception e) {
        if (e instanceof OSSException) {
            OSSException ossException = (OSSException) e;
            return errorCodeSet.contains(ossException.getErrCode());
        }
        return false;
    }

}

  ```
