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
package io.fabric8.kubernetes.client.dsl.internal.core.v1;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Gettable;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.dsl.base.OperationSupport;
import io.fabric8.kubernetes.client.utils.URLUtils;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServiceOperationsImpl extends HasMetadataOperation<Service, ServiceList, ServiceResource<Service>> implements ServiceResource<Service> {

  static final int HTTP_UNPROCESSABLE_ENTITY = 422;
  public static final String EXTERNAL_NAME = "ExternalName";

  public ServiceOperationsImpl(OkHttpClient client, Config config) {
    this(client, config, null);
  }

  public ServiceOperationsImpl(OkHttpClient client, Config config, String namespace) {
    this(new OperationContext().withOkhttpClient(client).withConfig(config).withNamespace(namespace).withPropagationPolicy(DEFAULT_PROPAGATION_POLICY));
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context.withPlural("services"));
    this.type = Service.class;
    this.listType = ServiceList.class;
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }
  
  @Override
  protected Service handleCreate(Service resource) throws ExecutionException, InterruptedException, IOException {
    try {
      return super.handleCreate(resource);
    } catch (KubernetesClientException e) {
      // repeated creates will fail with a 422, but client logic generally expects a 409
      if (e.getCode() == HTTP_UNPROCESSABLE_ENTITY 
          && fromServer(resource.getMetadata()) != null) {
        throw new KubernetesClientException(
            OperationSupport.createStatus(HttpURLConnection.HTTP_CONFLICT, "service already exists"));
      }
      throw e;
    }
  }

  @Override
  public Service replace(Service item) {
    return super.replace(handleClusterIp(item, fromServer(), "replace"));
  }

  @Override
  public Service patch(Service item) {
    return super.patch(handleClusterIp(item, this::getMandatory, "patch"));
  }

  @Override
  public Service waitUntilReady(long amount, TimeUnit timeUnit) throws InterruptedException {
    long started = System.nanoTime();
    super.waitUntilReady(amount, timeUnit);
    long alreadySpent = System.nanoTime() - started;

    // if awaiting existence took very long, let's give at least 10 seconds to awaiting readiness
    long remaining = Math.max(10_000, timeUnit.toNanos(amount) - alreadySpent);

    EndpointsOperationsImpl endpointsOperation = new EndpointsOperationsImpl(context);
    endpointsOperation.waitUntilReady(remaining, TimeUnit.MILLISECONDS);

    return get();
  }

  public String getURL(String portName) {
    String clusterIP = getMandatory().getSpec().getClusterIP();
    if ("None".equals(clusterIP)) {
      throw new IllegalStateException("Service: " + getMandatory().getMetadata().getName() + " in namespace "
          + namespace + " is head-less. Search for endpoints instead");
    }
    return getUrlHelper(portName);
  }

  private String getUrlHelper(String portName) {
    ServiceLoader<ServiceToURLProvider> urlProvider = ServiceLoader.load(ServiceToURLProvider.class,
        Thread.currentThread().getContextClassLoader());
    Iterator<ServiceToURLProvider> iterator = urlProvider.iterator();
    List<ServiceToURLProvider> servicesList = new ArrayList<>();

    while (iterator.hasNext()) {
      servicesList.add(iterator.next());
    }

    // Sort all loaded implementations according to priority
    Collections.sort(servicesList, new ServiceToUrlSortComparator());
    for (ServiceToURLProvider serviceToURLProvider : servicesList) {
      String url = serviceToURLProvider.getURL(getMandatory(), portName, namespace, new DefaultKubernetesClient(client, getConfig()));
      if (url != null && URLUtils.isValidURL(url)) {
        return url;
      }
    }

    return null;
  }

  private Pod matchingPod() {
    Service item = fromServer().get();
    Map<String, String> labels = item.getSpec().getSelector();
    PodList list = new PodOperationsImpl(client, config).inNamespace(item.getMetadata().getNamespace()).withLabels(labels).list();
    return list.getItems().stream().findFirst().orElseThrow(() -> new IllegalStateException("Could not find matching pod for service:" + item + "."));
  }

  @Override
  public PortForward portForward(int port, ReadableByteChannel in, WritableByteChannel out) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config)
        .inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName())
        .portForward(port, in, out);
  }

  @Override
  public LocalPortForward portForward(int port, int localPort) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config)
        .inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName())
        .portForward(port, localPort);
  }

  @Override
  public LocalPortForward portForward(int port) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config)
        .inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName())
        .portForward(port);
  }

  @Override
  public Service edit(Visitor... visitors) {
    return patch(new ServiceBuilder(getMandatory()).accept(visitors).build());
  }

  public class ServiceToUrlSortComparator implements Comparator<ServiceToURLProvider> {
    public int compare(ServiceToURLProvider first, ServiceToURLProvider second) {
      return first.getPriority() - second.getPriority();
    }
  }

  private Service handleClusterIp(Service item, Gettable<Service> current, String opType) {
    if (!isExternalNameService(item)) {
      try {
        Service old = current.get();
        return new ServiceBuilder(item)
          .editSpec()
          .withClusterIP(old.getSpec().getClusterIP())
          .endSpec()
          .build();
      } catch (Exception e) {
        throw KubernetesClientException.launderThrowable(forOperationType(opType), e);
      }
    }
    return item;
  }

  private boolean isExternalNameService(Service item) {
    if (item != null && item.getSpec() != null && item.getSpec().getType() != null) {
      return item.getSpec().getType().equals(EXTERNAL_NAME);
    }
    return false;
  }
}
