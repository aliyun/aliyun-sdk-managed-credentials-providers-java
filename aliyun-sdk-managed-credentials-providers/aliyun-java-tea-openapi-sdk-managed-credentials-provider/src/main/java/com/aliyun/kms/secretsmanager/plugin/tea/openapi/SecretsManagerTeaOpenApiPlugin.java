package com.aliyun.kms.secretsmanager.plugin.tea.openapi;

import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.kms.secretsmanager.plugin.common.*;
import com.aliyun.kms.secretsmanager.plugin.common.auth.SecretsManagerPluginCredentials;
import com.aliyun.kms.secretsmanager.plugin.common.utils.CredentialsUtils;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.Config;
import com.aliyuncs.kms.secretsmanager.client.exception.CacheSecretException;
import com.aliyuncs.kms.secretsmanager.client.model.SecretInfo;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecretsManagerTeaOpenApiPlugin {

    private static final Set<Class<? extends SecretsManagerPluginCredentialUpdater>> pluginCredentialUpdaterSet = new HashSet<>();

    private AliyunSDKSecretsManagerPlugin secretsManagerPlugin;

    private AKExpireHandler<TeaException> akExpireHandler = new TeaOpenApiPluginAKExpireHandler();

    static {
        pluginCredentialUpdaterSet.add(SecretsManagerTeaOpenApiCredentialUpdater.class);
    }

    /**
     * use for spring bean
     */
    public SecretsManagerTeaOpenApiPlugin() {
    }

    public SecretsManagerTeaOpenApiPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    /**
     * use for spring bean
     */
    public void setSecretsManagerPlugin(AliyunSDKSecretsManagerPlugin secretsManagerPlugin) {
        this.secretsManagerPlugin = secretsManagerPlugin;
    }

    public void setAkExpireHandler(AKExpireHandler<TeaException> akExpireHandler) {
        this.akExpireHandler = akExpireHandler;
    }

    public <T extends Client> T getTeaOpenApiClient(String endpoint, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        Config config = new Config();
        config.endpoint = endpoint;
        return getTeaOpenApiClient(config, clientClass, secretName);
    }

    public <T extends Client> T getTeaOpenApiClient(Config config, Class<? extends Client> clientClass, String secretName) throws CacheSecretException {
        SecretInfo secretInfo = secretsManagerPlugin.getSecretInfo(secretName);
        SecretsManagerPluginCredentials credentials = CredentialsUtils.generateCredentialsBySecret(secretInfo.getSecretValue());
        TeaOpenApiClientBuilder<T> teaOpenApiClientBuilder = new TeaOpenApiClientBuilder<T>(config, credentials, clientClass, secretName);
        T teaOpenApiClient = teaOpenApiClientBuilder.build();
        SecretsManagerTeaOpenApiCredentialUpdater<T> securityCredentialUpdater = new SecretsManagerTeaOpenApiCredentialUpdater<>(teaOpenApiClient);
        secretsManagerPlugin.registerSecretsManagerPluginUpdater(secretInfo.getSecretName(), securityCredentialUpdater);
        return teaOpenApiClient;
    }

    public <T extends Client> void closeOpenApiClient(T openApiClient, String secretName) throws IOException {
        String realSecretName = this.secretsManagerPlugin.getSecretName(secretName);
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(realSecretName, openApiClient);
    }

    public void destroy() throws IOException {
        secretsManagerPlugin.closeSecretsManagerPluginUpdaterAndClient(pluginCredentialUpdaterSet);
    }

    class TeaOpenApiClientBuilder<T extends Client> implements CloudClientBuilder<T> {
        private final SecretsManagerPluginCredentials credentials;
        private final Class<? extends Client> clientClass;
        private final Config config;
        private final String secretName;
        private static final String METHOD_NAME_DO_REQUEST = "doRequest";
        private static final String METHOD_NAME_DO_ROA_REQUEST_WITH_FORM = "doROARequestWithForm";
        private static final String METHOD_NAME_DO_ROA_REQUEST = "doROARequest";
        private static final String METHOD_NAME_DO_RPC_REQUEST = "doRPCRequest";
        private static final String METHOD_NAME_EXECUTE = "execute";

        public TeaOpenApiClientBuilder(Config config, SecretsManagerPluginCredentials credentials, Class<? extends Client> clientClass, String secretName) {
            this.credentials = credentials;
            this.clientClass = clientClass;
            this.config = config;
            this.secretName = secretName;
        }

        @Override
        public T build() {
            try {
                com.aliyun.credentials.models.Config credentialConfig = new com.aliyun.credentials.models.Config();
                credentialConfig.accessKeyId = credentials.getAccessKeyId();
                credentialConfig.accessKeySecret = credentials.getAccessKeySecret();
                credentialConfig.type = AuthConstant.ACCESS_KEY;
                config.credential = new com.aliyun.credentials.Client(credentialConfig);
                return (T) generateProxyClient(clientClass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Object generateProxyClient(Class<? extends Client> clientType) throws Exception {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(clientType);
            Set<String> methodsToProxy = new HashSet<>(Arrays.asList(
                    METHOD_NAME_DO_REQUEST,
                    METHOD_NAME_DO_ROA_REQUEST_WITH_FORM,
                    METHOD_NAME_DO_ROA_REQUEST,
                    METHOD_NAME_DO_RPC_REQUEST,
                    METHOD_NAME_EXECUTE
            ));
            enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
                if (methodsToProxy.contains(method.getName())) {
                    try {
                        return proxy.invokeSuper(obj, args);
                    } catch (TeaException e) {
                        if (akExpireHandler.judgeAKExpire(e)) {
                            AliyunSDKSecretsManagerPluginsManager.refreshSecretInfo(secretName);
                            return proxy.invokeSuper(obj, args);
                        }
                        throw e;
                    }
                } else {
                    return proxy.invokeSuper(obj, args);
                }
            });
            return enhancer.create(new Class[]{Config.class}, new Object[]{config});
        }
    }
}