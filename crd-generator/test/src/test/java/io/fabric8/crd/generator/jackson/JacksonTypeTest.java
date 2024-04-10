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
package io.fabric8.crd.generator.jackson;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JacksonTypeTest {

  @Test
  void testCrd() {
    CustomResourceDefinition d = Serialization.unmarshal(getClass().getClassLoader()
        .getResourceAsStream("META-INF/fabric8/examples.org.example-v1.yml"),
        CustomResourceDefinition.class);
    assertNotNull(d);
    assertEquals("Example", d.getSpec().getNames().getKind());
    Map<String, JSONSchemaProps> props = d.getSpec().getVersions().get(0).getSchema().getOpenAPIV3Schema().getProperties()
        .get("spec").getProperties();
    assertEquals(5, props.size());
    assertEquals("number", props.get("timestamp").getType());
    assertEquals("array", props.get("local").getType());
    assertEquals("string", props.get("uuid").getType());
    assertEquals("integer", props.get("shortValue").getType());
    assertEquals("boolean", props.get("ab").getType());
  }
}
