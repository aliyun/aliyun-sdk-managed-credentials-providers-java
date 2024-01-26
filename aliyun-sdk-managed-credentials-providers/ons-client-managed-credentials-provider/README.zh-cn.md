# 消息队列商业版TCP协议Java SDK托管凭据插件

消息队列商业版TCP协议Java SDK托管凭据插件可以使Java开发者通过托管RAM凭据快速使用阿里云ONS服务。您可以通过Maven快速使用。

*其他语言版本: [English](README.md), [简体中文](README.zh-cn.md)*

## 背景

当您的应用程序通过阿里云SDK访问阿里云消息队列时，访问凭证(Access Keys)被用于认证应用的身份。访问凭据在使用中存在一定的安全风险，可能会被恶意的开发人员或外部威胁所利用。

阿里云凭据管家提供了帮助降低风险的解决方案，允许企业和组织集中管理所有应用程序的访问凭据，允许在不中断应用程序的情况下自动或手动轮转或者更新这些凭据。托管在SecretsManager的Access
Key被称为[托管RAM凭据](https://help.aliyun.com/document_detail/212421.html) 。

使用凭据管家的更多优势，请参阅 [凭据管家概述](https://help.aliyun.com/document_detail/152001.html) 。

## 客户端机制

应用程序引用托管RAM凭据（Access Key）的`凭据名称` 。

托管凭据插件定期从SecretsManager获取由`凭据名称`代表的Access Key，并提供给阿里云SDK，应用则使用SDK访问阿里云消息队列。插件以指定的间隔（可配置）刷新缓存在内存中的Access Key。

在某些情况下，缓存的访问凭据不再有效，这通常发生在管理员在凭据管家中执行紧急访问凭据轮转以响应泄漏事件时。使用无效访问凭据调用阿里云消息队列通常会导致与API错误代码对应的异常。如果相应的Access Key无法使用错误，用户需要自行立即强制刷新缓存的Access Key，随后重试失败的API调用。请参阅[过期刷新处理程序](#过期刷新处理程序) 。

## 安装

可以通过Maven的方式在项目中通过凭据管家托管RAM凭据使用阿里云消息队列商业版TCP协议Java SDK。导入方式如下:

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
    <version>1.3.3</version>
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

您可以通过以下代码通过凭据管家托管RAM凭据使用阿里云ONS客户端。

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
        //自定义配置文件名称
        //ConfigLoader.setConfigName("your-config-name");
        // 获取Ons producer
        Producer producer = new ProxyOnsProducerBuilder().build(properties, secretName);
        producer.start();

        // 业务方业务代码：调用Ons Producer实现业务功能
        invoke(producer);

        // 通过下面方法关闭客户端来释放插件关联的资源
        producer.shutdown();
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
    <bean name="proxyOnsConsumerBuilder" class=" com.aliyun.kms.secretsmanager.plugin.ons.ProxyOnsProducerBuilder"/>
</beans>

```

## 过期刷新处理程序

当出现缓存的Access Key无法使用问题，用户需要自行立即强制刷新缓存的Access Key，随后重试失败的API调用，可以参考以下示例代码。

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
        //自定义配置文件名称
        //ConfigLoader.setConfigName("your-config-name");
        // 获取Ons producer
        Producer producer = new ProxyOnsProducerBuilder().build(properties, secretName);
        producer.start();

        // 业务方业务代码：调用Ons Producer实现业务功能
        Message msg = new Message("producer_retry", "retry", "Retry MQ producer".getBytes());

        try {
            SendResult sendResult = producer.send(msg);
        } catch (ONSClientException e) {
            System.out.println("发送失败");
            if (e.getMessage() != null && e.getMessage().contains("signature validate failed")) {
                AliyunSDKSecretsManagerPluginsManager.refreshSecretInfo(secretName);
                SendResult sendResult = producer.send(msg);
            }
        }

        // 通过下面方法关闭客户端来释放插件关联的资源
        producer.shutdown();
    }
}
```
