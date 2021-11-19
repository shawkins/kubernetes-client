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
package io.fabric8.istio.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.junit.rules.ExternalResource;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import okhttp3.mockwebserver.MockWebServer;

public class IstioServer extends ExternalResource {

  protected IstioMockServer mock;
  private IstioClient client;

  private final boolean https;
  // In this mode the mock web server will store, read, update and delete
  // kubernetes resources using an in memory map and will appear as a real api
  // server.
  private final boolean crudMode;

  public IstioServer() {
    this(true, false);
  }

  public IstioServer(boolean https) {
    this(https, false);
  }

  public IstioServer(boolean https, boolean crudMode) {
    this.https = https;
    this.crudMode = crudMode;
  }

  @Override
  public void before() {
    final Map<ServerRequest, Queue<ServerResponse>> responses = new HashMap<>();
    mock = crudMode
      ? new IstioMockServer(new Context(), new MockWebServer(), responses, new KubernetesMixedDispatcher(responses), true)
      : new IstioMockServer(https);
    mock.init();
    client = mock.createIstio();
  }

  @Override
  public void after() {
    mock.destroy();
    client.close();
  }

  public IstioClient getIstioClient() {
    return client;
  }

  public MockServerExpectation expect() {
    return mock.expect();
  }

  @Deprecated
  public <T> void expectAndReturnAsJson(String path, int code, T body) {
    expect().withPath(path).andReturn(code, body).always();
  }

  @Deprecated
  public void expectAndReturnAsString(String path, int code, String body) {
    expect().withPath(path).andReturn(code, body).always();
  }
}
