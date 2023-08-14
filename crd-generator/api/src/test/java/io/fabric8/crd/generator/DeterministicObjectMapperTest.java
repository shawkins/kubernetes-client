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
package io.fabric8.crd.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.fabric8.crd.generator.annotation.PrinterColumn;
import io.fabric8.crd.generator.v1.CustomResourceHandler;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceColumnDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicObjectMapperTest {

  @Test
  void checkSortingWithConverter() throws Exception {
    final JsonNode version = getCRDVersion(DeterministicObjectMapper.builder()
        .withConverter(TextNode.class, TextNode::textValue)
        .withConverter(CustomResourceColumnDefinition.class, CustomResourceColumnDefinition::getJsonPath)
        .build());

    // with the TestNode converter the order of the enum is deterministic
    final JsonNode unsortedEnum = getSchemaProperties(version).get("unsortedEnum").get("enum");
    assertEquals(Arrays.asList("A", "B", "C", "D"), Arrays.asList(
        unsortedEnum.get(0).textValue(),
        unsortedEnum.get(1).textValue(),
        unsortedEnum.get(2).textValue(),
        unsortedEnum.get(3).textValue()));

    // with the CustomResourceColumnDefinition converter the printer columns are sorted by jsonPath
    final JsonNode printerColumns = getPrinterColumns(version);
    assertEquals(".spec.unsortedEnum", printerColumns.get(0).get("jsonPath").textValue());
    assertEquals("Column B", printerColumns.get(0).get("name").textValue());
    assertEquals(".spec.value", printerColumns.get(1).get("jsonPath").textValue());
    assertEquals("Column A", printerColumns.get(1).get("name").textValue());
  }

  @Test
  void checkSortingWithReflection() throws Exception {
    final JsonNode version = getCRDVersion(DeterministicObjectMapper.builder()
        .build());

    // by default the order of the enum isn't deterministic
    final JsonNode unsortedEnum = getSchemaProperties(version).get("unsortedEnum").get("enum");
    assertNotEquals(Arrays.asList("A", "B", "C", "D"), Arrays.asList(
        unsortedEnum.get(0).textValue(),
        unsortedEnum.get(1).textValue(),
        unsortedEnum.get(2).textValue(),
        unsortedEnum.get(3).textValue()));

    // with reflection the printer columns are sorted by name
    final JsonNode printerColumns = getPrinterColumns(version);
    assertEquals(".spec.value", printerColumns.get(0).get("jsonPath").textValue());
    assertEquals("Column A", printerColumns.get(0).get("name").textValue());
    assertEquals(".spec.unsortedEnum", printerColumns.get(1).get("jsonPath").textValue());
    assertEquals("Column B", printerColumns.get(1).get("name").textValue());
  }

  private static JsonNode getCRDVersion(final ObjectMapper mapper) throws Exception {
    final Resources resources = new Resources();
    final CustomResourceHandler handler = new CustomResourceHandler(resources, false);
    final CustomResourceInfo info = CustomResourceInfo.fromClass(UnsortedResource.class);
    handler.handle(info);

    final String yaml = mapper.writeValueAsString(resources.generate().getItems());
    final JsonNode json = mapper.readTree(yaml);
    return json
        .get(0)
        .get("spec")
        .get("versions")
        .get(0);
  }

  private static JsonNode getSchemaProperties(final JsonNode json) {
    return json.get("schema")
        .get("openAPIV3Schema")
        .get("properties")
        .get("spec")
        .get("properties");
  }

  private static JsonNode getPrinterColumns(final JsonNode json) {
    return json.get("additionalPrinterColumns");
  }

  @Group("example.com")
  @Version("v1")
  @Kind("UnsortedResource")
  public static class UnsortedResource extends CustomResource<UnsortedSpec, Void> implements Namespaced {
  }

  /**
   * Has two fields annotated with {@link PrinterColumn}, so we can test the reflection
   */
  @SuppressWarnings("unused")
  public static class UnsortedSpec {

    @PrinterColumn(name = "Column A")
    private String value;

    @PrinterColumn(name = "Column B")
    private UnsortedEnum unsortedEnum;
  }

  private enum UnsortedEnum {
    D,
    B,
    C,
    A
  }
}
