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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
  
  // this size could be limited to max number of http connections
  private static final ExecutorService SHARED_POOL = Executors.newCachedThreadPool(ThreadUtils.daemonThreadFactory(Utils.class.getSimpleName()));
  private static final CachedSingleThreadScheduler SHARED_SCHEDULER = new CachedSingleThreadScheduler();

  private ThreadUtils() {
  }
  
  /**
   * Create a {@link ThreadFactory} with daemon threads and a thread
   * name based upon the object passed in.
   */
  public static ThreadFactory daemonThreadFactory(Object forObject) {
    String name = forObject.getClass().getSimpleName() + "-" + System.identityHashCode(forObject);
    return daemonThreadFactory(name);
  }

  static ThreadFactory daemonThreadFactory(String name) {
    return new ThreadFactory() {
      ThreadFactory threadFactory = Executors.defaultThreadFactory();
      
      @Override
      public Thread newThread(Runnable r) {
        Thread ret = threadFactory.newThread(r); 
        ret.setName(name + "-" + ret.getName());
        ret.setDaemon(true);
        return ret;
      }
    };
  }

  /**
   * Submit a {@link Runnable} to be executed in a common thread pool with a fixed delay.  The task should
   * be cancelled by the caller if needed.
   */
  public static Future<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    // just create and throw away the serial executor as it consumes no resources of its own
    return new SerialScheduledExecutorImpl(SHARED_POOL, SHARED_SCHEDULER).scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  /**
   * Submit a {@link Runnable} to be executed in a common thread pool.  The task should
   * be cancelled by the caller if needed.
   */
  public static Future<?> submit(Runnable command) {
    return SHARED_POOL.submit(command);
  }

  /**
   * Submit a {@link Callable} to be executed in a common thread pool.  The task should
   * be cancelled by the caller if needed.
   */
  public static <T> Future<T> submit(Callable<T> callable) {
    return SHARED_POOL.submit(callable);
  }

  /**
   * Create a new executor capable of scheduling which will execute all tasks in a serial manner off
   * of the shared pool
   */
  public static SerialScheduledExecutor newSerialScheduledExecutor() {
    return new SerialScheduledExecutorImpl(SHARED_POOL, SHARED_SCHEDULER);
  }

}
