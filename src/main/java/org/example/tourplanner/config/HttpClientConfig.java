package org.example.tourplanner.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import java.util.concurrent.TimeUnit;

public class HttpClientConfig {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final int TIMEOUT_SECONDS = 30;

    private static CloseableHttpClient httpClient;

    public static synchronized CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setResponseTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();
        }
        return httpClient;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static void closeHttpClient() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                // Log error
            }
        }
    }
}