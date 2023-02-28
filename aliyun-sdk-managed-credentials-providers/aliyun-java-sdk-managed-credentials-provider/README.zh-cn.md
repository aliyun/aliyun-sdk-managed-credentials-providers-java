# 阿里云Java SDK托管凭据插件

阿里云Java SDK的托管凭据插件可以帮助Java开发者更方便的利用托管在SecretsManager的RAM凭据，来访问阿里云服务的开放API。您可以通过Maven快速使用。

*其他语言版本: [English](README.md), [简体中文](README.zh-cn.md)*

## 背景
当您的应用程序通过阿里云SDK访问云服务时，访问凭证(Access Keys)被用于认证应用的身份。访问凭据在使用中存在一定的安全风险，可能会被恶意的开发人员或外部威胁所利用。

阿里云凭据管家提供了帮助降低风险的解决方案，允许企业和组织集中管理所有应用程序的访问凭据，允许在不中断应用程序的情况下自动或手动轮转或者更新这些凭据。托管在SecretsManager的Access Key被称为[托管RAM凭据](https://help.aliyun.com/document_detail/212421.html) 。

使用凭据管家的更多优势，请参阅 [凭据管家概述](https://help.aliyun.com/document_detail/152001.html) 。

## 客户端机制
应用程序引用托管RAM凭据（Access Key）的`凭据名称` 。

托管凭据插件定期从SecretsManager获取由`凭据名称`代表的Access Key，并提供给阿里云SDK，应用则使用SDK访问阿里云开放API。插件以指定的间隔（可配置）刷新缓存在内存中的Access Key。

在某些情况下，缓存的访问凭据不再有效，这通常发生在管理员在凭据管家中执行紧急访问凭据轮转以响应泄漏事件时。使用无效访问凭据调用OpenAPI通常会导致与API错误代码对应的异常。如果相应的错误代码为`InvalidAccessKeyId.NotFound`或`InvalidAccessKeyId`，则托管凭据插件将立即刷新缓存的Access Key，随后重试失败的OpenAPI调用。

如果使用过期Access Key调用某些云服务API返回的错误代码和上述所列错误码相异，应用开发人员则可以修改默认的错误重试行为。请参阅[修改默认过期处理程序](#修改默认过期处理程序) 。


## 安装

可以通过Maven的方式在项目中通过凭据管家托管RAM凭据使用阿里云Java SDK客户端。导入方式如下:

```XML
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-sdk-core</artifactId>
        <version>[4.3.2,5.0.0)</version>
    </dependency>
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-sdk-core-managed-credentials-provider</artifactId>
        <version>1.2.2</version>
    </dependency>
```
    
    
## 构建

您可以从Github检出代码通过下面的maven命令进行构建。

```
mvn clean install -DskipTests -Dgpg.skip=true
```

## 使用示例

### 步骤1：配置托管凭据插件

通过配置文件(`managed_credentials_providers.properties`)指定访问凭据管家([配置文件设置详情](../../README_config.zh-cn.md))，推荐采用Client Key方式访问凭据管家。
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

### 步骤 2：使用托管凭据插件访问云服务

您可以通过以下代码通过凭据管家托管RAM凭据使用阿里云SDK客户端。

```Java
import com.aliyun.kms.secretsmanager.plugin.sdkcore.ProxyAcsClient;
import com.aliyuncs.IAcsClient;

public class SdkProviderSample {
    
    public static void main(String[]args) throws Exception{
        String region="cn-hangzhou";
        String secretName="******";
        
        // 获取ACSClient
        IAcsClient client = new ProxyAcsClient(region,secretName);

        // 业务方业务代码：调用阿里云服务实现业务功能
        invoke(client,region);

        // 通过下面方法关闭客户端来释放插件关联的资源
        client.shutdown();
    }
}

```

### 步骤 2（替代方法）：集成Spring Bean

您也可以通过以下Spring Bean的方式快速集成。

```XML
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
      http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean name="proxyAcsClient" class="com.aliyun.kms.secretsmanager.plugin.sdkcore.ProxyAcsClient" destroy-method="shutdown">
        <constructor-arg name="regionId" value="${regionId}"></constructor-arg>
        <constructor-arg name="secretName" value="${secretName}"></constructor-arg>
    </bean>
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
