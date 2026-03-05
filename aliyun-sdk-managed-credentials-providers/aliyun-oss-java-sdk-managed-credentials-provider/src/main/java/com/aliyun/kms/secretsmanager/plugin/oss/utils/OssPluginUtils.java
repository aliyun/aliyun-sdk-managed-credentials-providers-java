package com.aliyun.kms.secretsmanager.plugin.oss.utils;

import com.aliyun.oss.ClientException;

import java.io.IOException;
import java.io.InputStream;

public class OssPluginUtils {
    public static final String PATH_FIELD_NAME = "path";

    public static void resetInputStream(InputStream original) {
        if (original != null) {
            if (original.markSupported()) {
                try {
                    original.reset();
                } catch (IOException ex) {
                    throw new ClientException("Cannot reset input stream:", ex);
                }
            }
        }
    }

}
