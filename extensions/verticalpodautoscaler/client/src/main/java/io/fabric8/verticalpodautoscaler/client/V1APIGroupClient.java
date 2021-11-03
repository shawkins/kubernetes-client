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
package io.fabric8.verticalpodautoscaler.client;

import io.fabric8.kubernetes.client.BaseClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.Handlers;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.verticalpodautoscaler.api.model.v1.VerticalPodAutoscaler;
import io.fabric8.verticalpodautoscaler.api.model.v1.VerticalPodAutoscalerCheckpoint;
import io.fabric8.verticalpodautoscaler.api.model.v1.VerticalPodAutoscalerCheckpointList;
import io.fabric8.verticalpodautoscaler.api.model.v1.VerticalPodAutoscalerList;
import io.fabric8.verticalpodautoscaler.client.dsl.V1APIGroupDSL;

public class V1APIGroupClient extends BaseClient implements V1APIGroupDSL {
  public V1APIGroupClient() {super();}

  public V1APIGroupClient(HttpClient httpClient, final Config config) {
    super(httpClient, config);
  }

  @Override
  public MixedOperation<VerticalPodAutoscaler, VerticalPodAutoscalerList, Resource<VerticalPodAutoscaler>> verticalpodautoscalers() {
    return Handlers.getOperation(VerticalPodAutoscaler.class, VerticalPodAutoscalerList.class, this.getHttpClient(),  this.getConfiguration());
  }

  @Override
  public MixedOperation<VerticalPodAutoscalerCheckpoint, VerticalPodAutoscalerCheckpointList, Resource<VerticalPodAutoscalerCheckpoint>> verticalpodautoscalercheckpoints() {
    return Handlers.getOperation(VerticalPodAutoscalerCheckpoint.class, VerticalPodAutoscalerCheckpointList.class, this.getHttpClient(),  this.getConfiguration());
  }
}
