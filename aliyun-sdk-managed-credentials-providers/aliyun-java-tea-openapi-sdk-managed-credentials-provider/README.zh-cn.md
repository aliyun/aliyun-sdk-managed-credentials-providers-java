# 阿里云 Tea OpenAPI Java SDK托管凭据插件

阿里云 Tea OpenAPI Java SDK的托管凭据插件可以帮助Java开发者更方便的利用托管在SecretsManager的RAM凭据，来访问阿里云服务的开放API。您可以通过Maven快速使用。[查看可使用的云产品列表](https://github.com/aliyun/alibabacloud-java-sdk)

*其他语言版本: [English](README.md), [简体中文](README.zh-cn.md)*

## 背景

当您的应用程序通过阿里云 Tea OpenAPI SDK访问云服务时，访问凭证(Access Keys)被用于认证应用的身份。访问凭据在使用中存在一定的安全风险，可能会被恶意的开发人员或外部威胁所利用。

阿里云凭据管家提供了帮助降低风险的解决方案，允许企业和组织集中管理所有应用程序的访问凭据，允许在不中断应用程序的情况下自动或手动轮转或者更新这些凭据。托管在SecretsManager的Access
Key被称为[托管RAM凭据](https://help.aliyun.com/document_detail/212421.html) 。

使用凭据管家的更多优势，请参阅 [凭据管家概述](https://help.aliyun.com/document_detail/152001.html) 。

## 客户端机制

应用程序引用托管RAM凭据（Access Key）的`凭据名称` 。

托管凭据插件定期从SecretsManager获取由`凭据名称`代表的AccessKey，并提供给阿里云SDK，应用则使用SDK访问阿里云开放API。插件以指定的间隔（可配置）刷新缓存在内存中的Access Key。

在某些情况下，缓存的访问凭据不再有效，这通常发生在管理员在凭据管家中执行紧急访问凭据轮转以响应泄漏事件时。使用无效访问凭据调用OpenAPI通常会导致与API错误代码对应的异常。如果相应的错误代码为`InvalidAccessKeyId.NotFound`、`InvalidAccessKeyId`或`Unauthorized`，则托管凭据插件将立即刷新缓存的Access Key，随后重试失败的OpenAPI调用。

如果使用过期AccessKey调用某些云服务API返回的错误代码和上述所列错误码相异，应用开发人员则可以修改默认的错误重试行为。请参阅[修改默认过期处理程序](#修改默认过期处理程序) 。

## 安装

可以通过Maven的方式在项目中通过凭据管家托管RAM凭据使用阿里云Java SDK客户端。导入方式如下:

```XML

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-tea-openapi-sdk-managed-credentials-provider</artifactId>
    <version>1.3.5</version>
</dependency>
```

## 构建

您可以从Github检出代码通过下面的maven命令进行构建。

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## 使用示例(以ECS为例)

### 步骤 1：安装ECS SDK

```XML

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>ecs20140526</artifactId>
    <version>7.1.0</version>
</dependency>
```

### 步骤 2：配置托管凭据插件

通过配置文件(`managed_credentials_providers.properties`)
指定访问凭据管家([配置文件设置详情](../../README_config.zh-cn.md))，推荐采用Client Key方式访问凭据管家。

```properties
## 访问凭据类型
credentials_type=client_key
## 读取client key的解密密码：支持从环境变量或者文件读取
client_key_password_from_env_variable=#your client key private key password environment variable name#
##client_key_password_from_file_path=#your client key private key password file path#
## Client Key的私钥文件
client_key_private_key_path=#your client key private key file path#
## 关联的凭据管家服务地域
cache_client_region_id=[{"regionId":"#regionId#"}]
```

### 步骤 3：使用托管凭据插件访问云服务

您可以通过以下代码通过凭据管家托管RAM凭据使用阿里云SDK客户端。

```Java
import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeInstancesResponse;
import com.aliyun.kms.secretsmanager.plugin.tea.openapi.ProxyClientCreator;
import com.google.gson.Gson;

public class AliyunTeaOpenApiProviderSample {

    public static void main(String[] args) throws Exception {
        // Step 1: 指定托管凭据的名称（在 SecretsManager 中创建的凭据名称）
        String secretName = "your-secret-name";

        // Step 2: 配置 OpenAPI 客户端
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.endpoint = "your-product-endpoint"; // 替换为实际的云产品 endpoint

        // Step 3: 创建客户端实例
        Client client = ProxyClientCreator.createClient(config, Client.class, secretName);

        // Step 4: 调用云服务 API
        com.aliyun.ecs20140526.models.DescribeInstancesRequest request = new com.aliyun.ecs20140526.models.DescribeInstancesRequest();
        request.setRegionId("cn-hangzhou"); // 设置区域 ID
        DescribeInstancesResponse response = client.describeInstances(request);

        // Step 5: 打印结果
        System.out.println(new Gson().toJson(response.getBody()));
    }
}

```

## 修改默认过期处理程序

在支持用户自定义错误重试的托管凭据Java插件中，用户可以自定义客户端因凭据手动轮转极端场景下的错误重试判断逻辑，只实现以下接口即可。

  ```Java
package com.aliyun.kms.secretsmanager.plugin.common;

public interface AKExpireHandler<TException> {

    /**
     * 判断异常是否由AK过期引起
     *
     * @param e
     * @return
     */
    boolean judgeAKExpire(TException e);
}

  ```

下面代码示例是用户自定义判断异常接口和使用自定义判断异常实现访问云服务。

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
        // Step 1: 指定托管凭据的名称（在 SecretsManager 中创建的凭据名称）
        String secretName = "your-secret-name";

        // Step 2: 配置 OpenAPI 客户端
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.endpoint = "your-product-endpoint"; // 替换为实际的云产品 endpoint

        // Step 3: 创建AK过期处理器
        TeaOpenApiAKExpireHandler akExpireHandler = new TeaOpenApiAKExpireHandler(new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId", "Unauthorized"});

        // Step 4: 创建客户端实例
        Client client = ProxyClientCreator.createClient(config, Client.class, secretName, akExpireHandler);

        // Step 5: 调用云服务 API
        com.aliyun.ecs20140526.models.DescribeInstancesRequest request = new com.aliyun.ecs20140526.models.DescribeInstancesRequest();
        request.setRegionId("cn-hangzhou"); // 设置区域 ID
        DescribeInstancesResponse response = client.describeInstances(request);

        // Step 6: 打印结果
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
