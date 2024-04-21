/*
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
package io.fabric8.crd.generator.utils;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

public class Types {
  private Types() {
    throw new IllegalStateException("Utility class");
  }

  public static class SpecAndStatus {

    private final String specClassName;
    private final String statusClassName;
    private final boolean unreliable;

    public SpecAndStatus(String specClassName, String statusClassName) {
      this.specClassName = specClassName;
      this.statusClassName = statusClassName;
      this.unreliable = specClassName == null || statusClassName == null;
    }

    public String getSpecClassName() {
      return specClassName;
    }

    public String getStatusClassName() {
      return statusClassName;
    }

    public boolean isUnreliable() {
      return unreliable;
    }
  }

  public static SpecAndStatus resolveSpecAndStatusTypes(Class<?> definition) {
    SerializationConfig config = new ObjectMapper().getSerializationConfig();
    BeanDescription description = config.introspect(config.constructType(definition));
    String specClassName = null;
    String statusClassName = null;
    for (BeanPropertyDefinition bpd : description.findProperties()) {
      if (bpd.getName().equals("spec")) {
        specClassName = bpd.getRawPrimaryType().getName();
      } else if (bpd.getName().equals("status")) {
        statusClassName = bpd.getRawPrimaryType().getName();
      }
    }
    return new SpecAndStatus(specClassName, statusClassName);
  }

}
