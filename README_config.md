 # Configuration File Setting For The Aliyun Security Client 

You can use the configuration file named `managed_credentials_providers.properties` (it exists in the program running directory) to initialize the Aliyun SDK Managed Credentials Providers:

1. Use ECS RAM role to access Aliyun KMS,you must set the following configuration variables

```
## the type of access credentials
credentials_type=ecs_ram_role
## ecs instance RAM Role name
credentials_role_name=#credentials_role_name#
## the region information
cache_client_region_id=[{"regionId":"#regionId#"}]
## the custom refresh time interval of the secret, by default 6 hour, the minimum value is 5 minutes，the time unit is milliseconds
## the config item to set 1 hour with the custom refresh time interval of the secret 
refresh_secret_ttl=3600000
```

2. Use Client Key to access Aliyun KMS,you must set the following configuration variables

``` 
## the type of access credentials
credentials_type=client_key

## you could read the password of client key from environment variable or file
client_key_password_from_env_variable=#your client key private key password environment variable name#


client_key_password_from_file_path=#your client key private key password file path#
## the private key file path of the Client Key
client_key_private_key_path=#your client key private key file path#

## the region related to the kms service
cache_client_region_id=[{"regionId":"#regionId#"}]

## the custom refresh time interval of the secret, by default 6 hour, the minimum value is 5 minutes，the time unit is milliseconds
## the config item to set 1 hour with the custom refresh time interval of the secret 
refresh_secret_ttl=3600000
```
