package com.litovskiy.config;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Configuration
public class ProxyConfiguration {

    @Value("${proxy.enabled:false}")
    private boolean isProxyEnabled;

    @Value("${proxy.username:}")
    private String username;

    @Value("${proxy.password:}")
    private String password;

    @Value("${proxy.host:}")
    private String host;

    @Value("${proxy.port:0}")
    private int port;

    @Bean
    public OkHttpClient getOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (isProxyEnabled) {
            validateProxyConfiguration();
            builder.proxy(getProxy())
                .proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
                });
        }

        return addTimeouts(builder).build();
    }

    private void validateProxyConfiguration() {
        if (host == null || host.isBlank() || port <= 0) {
            throw new IllegalStateException(
                "proxy.enabled=true, but proxy.host and proxy.port are not configured"
            );
        }
    }

    private Proxy getProxy() {
        InetSocketAddress address = new InetSocketAddress(host, port);
        return new Proxy(Proxy.Type.HTTP, address);
    }

    private OkHttpClient.Builder addTimeouts(OkHttpClient.Builder builder) {
        return builder.callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true);
    }
}
