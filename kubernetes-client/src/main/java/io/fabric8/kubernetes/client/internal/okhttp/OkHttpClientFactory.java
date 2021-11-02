/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubernetes.client.internal.okhttp;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OkHttpClientFactory implements HttpClient.Factory {

  @Override
  public HttpClient createHttpClient(Config config) {
    return createHttpClient(config, builder -> {});
  }
  
  @Override
  public Builder newBuilder() {
    return new OkHttpClientBuilderImpl(new OkHttpClient.Builder());
  }

  /**
   * Creates an HTTP client configured to access the Kubernetes API.
   * @param config Kubernetes API client config
   * @param additionalConfig a consumer that allows overriding HTTP client properties
   * @return returns an HTTP client
   */
  public static io.fabric8.kubernetes.client.http.HttpClient createHttpClient(Config config,
      final Consumer<OkHttpClient.Builder> additionalConfig) {
    try {
      OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

      if (config.isTrustCerts() || config.isDisableHostnameVerification()) {
        httpClientBuilder.hostnameVerifier((s, sslSession) -> true);
      }

      Logger reqLogger = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
      if (reqLogger.isTraceEnabled()) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClientBuilder.addNetworkInterceptor(loggingInterceptor);
      }

      if (config.getWebsocketPingInterval() > 0) {
        httpClientBuilder.pingInterval(config.getWebsocketPingInterval(), TimeUnit.MILLISECONDS);
      }

      if (config.getMaxConcurrentRequests() > 0 && config.getMaxConcurrentRequestsPerHost() > 0) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(config.getMaxConcurrentRequests());
        dispatcher.setMaxRequestsPerHost(config.getMaxConcurrentRequestsPerHost());
        httpClientBuilder.dispatcher(dispatcher);
      }

      Builder builderWrapper = new OkHttpClientBuilderImpl(httpClientBuilder);

      HttpClientUtils.applyCommonConfiguration(config, builderWrapper);
      
      if (shouldDisableHttp2() && !config.isHttp2Disable()) {
        builderWrapper.preferHttp11();
      }
      
      if (additionalConfig != null) {
        additionalConfig.accept(httpClientBuilder);
      }

      return builderWrapper.build();
    } catch (Exception e) {
      throw KubernetesClientException.launderThrowable(e);
    }
  }
  
  /**
   * OkHttp wrongfully detects >JDK8u251 as {@link okhttp3.internal.platform.Jdk9Platform} which enables Http2
   * unsupported for JDK8.
   *
   * @return true if JDK8 is detected, false otherwise-
   * @see <a href="https://github.com/fabric8io/kubernetes-client/issues/2212">#2212</a>
   */
  private static boolean shouldDisableHttp2() {
      return System.getProperty("java.version", "").startsWith("1.8");
  }

}
