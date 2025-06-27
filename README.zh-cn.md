![](https://aliyunsdk-pages.alicdn.com/icons/AlibabaCloud.svg)
# 阿里云SDK托管凭据Java插件

[![GitHub version](https://badge.fury.io/gh/aliyun%2Faliyun-sdk-managed-credentials-providers-java.svg)](https://badge.fury.io/gh/aliyun%2Faliyun-sdk-managed-credentials-providers-java)

阿里云SDK托管凭据Java插件可以使Java开发者通过托管RAM凭据快速使用阿里云服务。您可以通过Maven快速使用。

*其他语言版本: [English](README.md), [简体中文](README.zh-cn.md)*

- [阿里云托管RAM凭据主页](https://help.aliyun.com/document_detail/212421.html)
- [Issues](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/issues)
- [Release](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/releases)

## 许可证

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)


## 优势
* 支持用户快速通过托管RAM凭据快速使用阿里云服务
* 支持多种认证鉴权方式如ECS实例RAM Role和Client Key
* 支持阿里云服务客户端自动刷新AK信息

## 软件要求

- 您的凭据必须是托管RAM凭据
- Java 1.8 或以上版本
- Maven

## 背景
当您通过阿里云SDK访问服务时，访问凭证(Access Keys)是被用于认证用户身份. 然而访问凭据容易在使用中存在安全风险，可能会被敌对的开发人员或外部威胁所利用.

阿里云凭据管家具备一种帮助降低风险的解决方案，它允许组织集中管理所有应用程序的访问凭据，允许在不中断应用程序的情况下自动或手动旋转这些凭据。 凭据管家中的托管访问凭据称为[托管RAM凭据](https://help.aliyun.com/document_detail/212421.html) 。

有关使用凭据管家的更多优势信息，请参阅 [凭据管家概述](https://help.aliyun.com/document_detail/152001.html) 。

## 客户端机制
应用程序使用由凭据管家通过代表访问凭据的`凭据名称`管理的访问凭据。

托管凭证插件在访问阿里云开放API时，定期获取由`凭据名称`表示的访问凭据，并提供给阿里云SDK。提供程序通常以指定的间隔（可配置）刷新本地缓存的访问凭据。

在某些情况下，缓存的访问凭据不再有效，这通常发生在管理员在凭据管家中执行紧急访问凭据轮转以响应泄漏事件时。使用无效访问凭据调用OpenAPI通常会导致与API错误代码对应的异常。如果相应的错误代码为`InvalidAccessKeyId.NotFound`或`InvalidAccessKeyId`，则托管凭据插件提供程序将立即刷新缓存的访问评剧馆并重试失败的OpenAPI。

如果API返回使用过期访问凭据的其他错误代码，应用程序开发人员可以覆盖或扩展特定云服务的此行为。请参阅[修改默认过期处理程序](#修改默认过期处理程序) 。



## 安装

可以通过Maven的方式在项目中使用阿里云SDK托管凭据Java插件，云产品依赖按需引入。下面以OSS插件导入方式如下:

```
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-sdk-oss-managed-credentials-provider</artifactId>
    <version>1.3.5</version>
</dependency>
```


## 构建

您可以从Github检出代码通过下面的maven命令进行构建。

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## 支持阿里云云产品

阿里云SDK托管凭据Java插件支持以下云产品:

|       阿里云SDK名称       | SDK maven(groupId:artifactId) |           支持版本            | 插件名称 | 导入插件maven(groupId:artifactId)  | 
|:--------------------:| :----: |:-------------------------:|  :----: | :----: |
|        阿里云SDK        | com.aliyun:aliyun-java-sdk-core |       4.3.2~4.5.17        | [阿里云Java SDK托管凭据插件](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/aliyun-java-sdk-managed-credentials-provider) | com.aliyun:aliyun-java-sdk-core-managed-credentials-provider  | 
|     OSS Java SDK     | com.aliyun.oss:aliyun-sdk-oss |       2.1.0~3.10.2        | [OSS Java SDK托管凭据插件](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/aliyun-oss-java-sdk-managed-credentials-provider) | com.aliyun:aliyun-sdk-oss-managed-credentials-provider  | 
| 消息队列商业版TCP协议Java SDK | com.aliyun.openservices:ons-client | 1.8.5.Final~1.8.7.4.Final | [消息队列商业版TCP协议Java SDK托管凭据插件](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/ons-client-managed-credentials-provider) | com.aliyun:ons-client-managed-credentials-provider  | 
|  阿里云Tea OpenAPI SDK  | com.aliyun:tea-openapi |          >=0.0.1          | [阿里云 Tea OpenAPI Java SDK托管凭据插件](https://github.com/aliyun/aliyun-sdk-managed-credentials-providers-java/tree/master/aliyun-sdk-managed-credentials-providers/aliyun-java-tea-openapi-sdk-managed-credentials-provider) | aliyun-java-tea-openapi-sdk-managed-credentials-provider  | 



## 使用凭据管家托管RAM凭据方式访问云产品

### 步骤1 配置托管凭据插件

通过配置文件(`managed_credentials_providers.properties`)指定访问凭据管家([配置文件设置详情](./README_config.zh-cn.md))，推荐采用Client Key方式访问凭据管家

```properties
## 访问凭据类型
credentials_type=client_key

## 读取client key的解密密码：支持从环境变量或者文件读取
client_key_password_from_env_variable=#your client key private key password environment variable name#
client_key_password_from_file_path=#your client key private key password file path#

## Client Key的私钥文件
client_key_private_key_path=#your client key private key file path#

## 关联的KMS服务地域
cache_client_region_id=[{"regionId":"#regionId#"}]
```


### 步骤 2 使用托管凭据插件访问云服务

下面以托管RAM凭据访问OSS服务为例。

```Java
import com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.Bucket;
import com.aliyun.kms.secretsmanager.plugin.common.utils.ConfigLoader;
import java.util.List;
    
public class OssPluginSample {

    public static void main(String[] args) throws Exception {
        String secretName = "******";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        //自定义配置文件名称
        //ConfigLoader.setConfigName("your-config-name");
        // 获取Oss Client
        OSS ossClient = new ProxyOSSClientBuilder().build(endpoint, secretName);

        List<Bucket> buckets = ossClient.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket != null) {
                // do something with bucket
            }
        }

        // 通过下面方法关闭客户端来释放插件关联的资源
         ossClient.shutdown();
    }
}
```

### 步骤 2(替代方法) 集成Spring Bean

您也可以通过以下Spring Bean的方式快速集成阿里云SDK（下面以Oss SDK为例）。

```XML
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
              http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean name="proxyOSSClientBuilder" class="com.aliyun.kms.secretsmanager.plugin.oss.ProxyOSSClientBuilder" scope="singleton" />
</beans>

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

        //自定义配置文件名称
        //ConfigLoader.setConfigName("your-config-name");
        // 获取 SDK Core客户端
        // 指定特定的错误码进行重新获取凭据值
        IAcsClient client = new ProxyAcsClient(region, secretName, new AliyunSdkAKExpireHandler(new String[]{"InvalidAccessKeyId.NotFound", "InvalidAccessKeyId"}));

        // 业务代码
        invoke(client,region);

        // 通过下面方法关闭客户端来释放插件关联的资源 
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

 
