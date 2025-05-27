package org.example.tourplanner.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import java.util.concurrent.TimeUnit;

public class HttpClientConfig {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final int TIMEOUT_SECONDS = 30;


    /**
     * Erstellt einen neuen HttpClient für jeden Request
     * Das vermeidet  "Connection pool shut down" Problem
     */
    public static CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setResponseTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }


    @Deprecated
    public static synchronized CloseableHttpClient getHttpClient() {
        return createHttpClient();
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    // Diese Methode ist nicht mehr notwendig
    // public static void closeHttpClient() { ... }
}