# 阿里云托管凭据客户端配置文件设置 

`managed_credentials_providers.properties`(在程序运行目录下)初始化阿里云凭据管家动态RAM凭据客户端：

1. 采用阿里云ECS Ram Role作为访问鉴权方式

    ```properties
credentials_type=ecs_ram_role
## ECS RAM Role名称
credentials_role_name=#credentials_role_name#
## 关联的KMS服务地域
cache_client_region_id=[{"regionId":"#regionId#"}]
## 用户自定义的刷新频率, 默认为6小时，最小值为5分钟，单位为毫秒
## 下面的配置将凭据刷新频率设定为1小时
refresh_secret_ttl=3600000
    ```

2. 采用阿里云Client Key作为访问鉴权方式

    ```properties
## 访问凭据类型
credentials_type=client_key

## 读取client key的解密密码：支持从环境变量或者文件读取
client_key_password_from_env_variable=#your client key private key password environment variable name#
client_key_password_from_file_path=#your client key private key password file path#

# Client Key私钥文件路径
client_key_private_key_path=#your client key private key file path#

## 关联的KMS服务地域
cache_client_region_id=[{"regionId":"#regionId#"}]
## 用户自定义的刷新频率, 默认为6小时，最小值为5分钟，单位为毫秒
## 下面的配置将凭据刷新频率设定为1小时
refresh_secret_ttl=3600000
    ```

