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

package io.fabric8.kubernetes.client;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class Handlers {

  private static final Map<Class<?>, ResourceHandler<?, ?>> RESOURCE_HANDLER_MAP = new ConcurrentHashMap<>();
  
  private Handlers() {
    //Utility
  }

  public static <T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> void register(Class<T> type, BiFunction<OkHttpClient, Config, HasMetadataOperation<T, L, R>> operationConstructor) {
    if (RESOURCE_HANDLER_MAP.put(type, new ResourceHandlerImpl(type, operationConstructor)) != null) {
      throw new AssertionError(String.format("%s already registered", type.getName()));
    }
  }
  
  public static <T extends HasMetadata> void unregister(Class<T> type) {
    RESOURCE_HANDLER_MAP.remove(type);
  }

  public static <T extends HasMetadata, V extends VisitableBuilder<T, V>> ResourceHandler<T, V> get(T meta) {
    // could try to return something here in the generic case, but there is no generic builder
    return get((Class<T>)meta.getClass());
  }

  public static <T extends HasMetadata, V extends VisitableBuilder<T, V>> ResourceHandler<T, V> get(Class<T> type) {
    if (type.equals(GenericKubernetesResource.class)) {
      return null;
    }
    return (ResourceHandler<T, V>) RESOURCE_HANDLER_MAP.computeIfAbsent(type, k -> new ResourceHandlerImpl<>(type, null));
  }
  
  public static <T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> HasMetadataOperation<T, L, R> getOperation(Class<T> type, Class<L> listType, OkHttpClient client, Config config) {
    ResourceHandler<T, ?> resourceHandler = get(type);
    return (HasMetadataOperation<T, L, R>) resourceHandler.operation(client, config, listType);
  }
  
  public static <T extends HasMetadata> boolean hasDefaultOperation(Class<T> type) {
    ResourceHandler handler = RESOURCE_HANDLER_MAP.get(type);
    if (handler != null) {
      return !handler.hasOperation();
    }
    return false;
  }

}
