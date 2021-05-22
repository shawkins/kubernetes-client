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

package io.fabric8.kubernetes.client.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class SerialScheduledExecutorImpl extends SerialExecutor implements SerialScheduledExecutor {

  private boolean shutdown;
  private final CachedSingleThreadScheduler scheduler; // not used for execution, only scheduling
  private final Set<CompletableFuture<?>> scheduled = Collections.newSetFromMap(new IdentityHashMap<>());

  public SerialScheduledExecutorImpl(Executor executor, CachedSingleThreadScheduler sharedScheduler) {
    super(executor);
    this.scheduler = sharedScheduler;
  }

  @Override
  public synchronized void terminate() {
    if (shutdown) {
      return;
    }
    shutdown = true;
    tasks.clear();
    if (activeThread != null) {
      activeThread.interrupt();
    }
    scheduled.forEach(f -> f.cancel(true));
    scheduled.clear();
  }

  @Override
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  @Override
  public synchronized void execute(Runnable command) {
    if (shutdown) {
      throw new RejectedExecutionException();
    }
    super.execute(command);
  }

  @Override
  public synchronized Future<?> schedule(Runnable command, long delay, TimeUnit timeUnit) {
    if (shutdown) {
      throw new RejectedExecutionException();
    }
    // schedule with the common scheduler, but track the future for termination
    CompletableFuture<?> result = new CompletableFuture<>();
    ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
      try {
        this.execute(command);
        result.complete(null);
      } catch (RuntimeException e) {
        result.completeExceptionally(e);
        throw e;
      }
    }, delay, timeUnit);
    scheduled.add(result);
    result.whenComplete((o, t) -> {
      scheduledFuture.cancel(true);
      synchronized (this) {
        scheduled.remove(result);        
      }
    });
    return result;
  }

  @Override
  public Future<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    // scheduleWithFixedDelay is slightly complicated given the hand off to another executor
    CompletableFuture<?> result = new CompletableFuture<>();
    AtomicReference<Future<?>> futureReference = new AtomicReference<>();
    futureReference.set(schedule(new Runnable() {
      @Override
      public void run() {
        try {
          command.run();
          futureReference.set(schedule(this, delay, unit));
        } catch (RuntimeException e) {
          result.completeExceptionally(e);
          throw e;
        }
      }
    }, initialDelay, unit));
    result.whenComplete((o, t) -> {
      futureReference.get().cancel(false);
    });
    return result;
  }

}
