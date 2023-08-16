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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Value.construct;

/**
 * An {@link ObjectMapper} that serializes YAML in a deterministic way.
 *
 * Converts unsorted collections into sorted lists, using the provided converters.
 */
class DeterministicObjectMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeterministicObjectMapper.class);

  static DeterministicObjectMapperBuilder builder() {
    return new DeterministicObjectMapperBuilder();
  }

  /**
   * Builds a deterministic {@link ObjectMapper} with custom converters.
   */
  static class DeterministicObjectMapperBuilder {

    private DeterministicObjectMapperBuilder() {
    }

    /**
     * @return the configured {@link DeterministicObjectMapper} instance
     */
    ObjectMapper build() {
      return JsonMapper.builder(new YAMLFactory()
          .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
          .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
          .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .withConfigOverride(Map.class, configOverride -> configOverride.setInclude(construct(NON_NULL, NON_NULL)))
          .serializationInclusion(NON_EMPTY)
          .build();
    }

  }

}
