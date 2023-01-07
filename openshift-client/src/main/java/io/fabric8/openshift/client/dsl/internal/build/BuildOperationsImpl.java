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
package io.fabric8.openshift.client.dsl.internal.build;

import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.TimestampBytesLimitTerminateTimeTailPrettyLoggable;
import io.fabric8.kubernetes.client.extension.LoggableExtensibleResourceAdapter;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.dsl.BuildResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildOperationsImpl extends LoggableExtensibleResourceAdapter<Build> implements
    BuildResource {

  public static final String OPENSHIFT_IO_BUILD_NAME = "openshift.io/build.name";
  private Integer version;

  @Override
  protected String getLogParameters() {
    String params = super.getLogParameters();
    if (version != null) {
      params += ("&version=" + version);
    }
    return params;
  }

  @Override
  public BuildOperationsImpl newInstance() {
    return new BuildOperationsImpl().version(version);
  }

  BuildOperationsImpl version(Integer version) {
    this.version = version;
    return this;
  }

  @Override
  public TimestampBytesLimitTerminateTimeTailPrettyLoggable withVersion(Integer version) {
    BuildOperationsImpl result = newInstance().version(version);
    result.init(result, client);
    return result;
  }

  @Override
  protected List<PodResource> getPodsToWaitFor() {

    return null;
  }

  static Map<String, String> getBuildPodLabels(Build build) {
    Map<String, String> labels = new HashMap<>();
    if (build != null && build.getMetadata() != null) {
      labels.put(OPENSHIFT_IO_BUILD_NAME, build.getMetadata().getName());
    }
    return labels;
  }
}
