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

package io.fabric8.kubernetes.client.extension;

import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.TimestampBytesLimitTerminateTimeTailPrettyLoggable;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

public abstract class LoggableExtensibleResourceAdapter<T> extends ExtensibleResourceAdapter<T>
    implements TimestampBytesLimitTerminateTimeTailPrettyLoggable {

  protected LoggingContext loggingContext = new LoggingContext();

  @Override
  public abstract LoggableExtensibleResourceAdapter<T> newInstance();

  public LoggableExtensibleResourceAdapter<T> initialized(LoggingContext context) {
    LoggableExtensibleResourceAdapter<T> result = newInstance();
    result.loggingContext = context;
    result.init(result, client);
    return result;
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> limitBytes(int limitBytes) {
    return initialized(loggingContext.toBuilder().limitBytes(limitBytes).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> terminated() {
    return initialized(loggingContext.toBuilder().previous(true).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> sinceTime(String timestamp) {
    return initialized(loggingContext.toBuilder().sinceTime(timestamp).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> sinceSeconds(int seconds) {
    return initialized(loggingContext.toBuilder().sinceSeconds(seconds).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> tailingLines(int lines) {
    return initialized(loggingContext.toBuilder().tailLines(lines).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> withPrettyOutput() {
    return initialized(loggingContext.toBuilder().pretty(true).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> withLogWaitTimeout(Integer logWaitTimeout) {
    return withReadyWaitTimeout(logWaitTimeout);
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> withReadyWaitTimeout(Integer timeout) {
    return initialized(loggingContext.toBuilder().readyWaitTimeout(timeout).build());
  }

  @Override
  public LoggableExtensibleResourceAdapter<T> usingTimestamps() {
    return initialized(loggingContext.toBuilder().timestamps(true).build());
  }

  @Override
  public String getLog() {
    return doGetLog(String.class);
  }

  @Override
  public String getLog(boolean isPretty) {
    return initialized(loggingContext.toBuilder().pretty(isPretty).build()).getLog();
  }

  @Override
  public Reader getLogReader() {
    return doGetLog(Reader.class);
  }

  @Override
  public InputStream getLogInputStream() {
    return doGetLog(InputStream.class);
  }

  @Override
  public LogWatch watchLog() {
    return watchLog(null);
  }

  @Override
  public LogWatch watchLog(OutputStream out) {
    // TODO Auto-generated method stub
    return null;
  }

  protected abstract List<PodResource> getPodsToWaitFor();

  private void waitUntilPodsBecomesReady() {
    List<PodResource> podOps = getPodsToWaitFor();

    //PodOperationUtil.waitUntilReadyOrSucceded(podOp, podLogWaitTimeout);
    //waitForBuildPodToBecomeReady(podOps,
    //    operationContext.getReadyWaitTimeout() != null ? operationContext.getReadyWaitTimeout() : DEFAULT_POD_LOG_WAIT_TIMEOUT);
  }

  private <T> T doGetLog(Class<T> type) {
    return this.resource.operation(type, b -> b.parameters(loggingContext));
  }

}
