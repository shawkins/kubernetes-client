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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private final CustomComparators customComparators = new CustomComparators();

    private DeterministicObjectMapperBuilder() {
    }

    /**
     * Adds a custom converter for the given class, using the given key extractor.
     * 
     * Creates a {@link Comparator} that returns {@code 0} for {@code null} values.
     * 
     * @param clazz the class to compare
     * @param keyExtractor the key to used for comparison
     * @return the builder
     */
    <T, U extends Comparable<? super U>> DeterministicObjectMapperBuilder withConverter(final Class<T> clazz,
        final Function<? super T, ? extends U> keyExtractor) {
      return withConverter(clazz, comparingNullable(keyExtractor));
    }

    /**
     * Adds a custom converter for the given class, using the given key extractor.
     *
     * Creates a {@link Comparator} that returns {@code 0} for {@code null} values.
     * 
     * @param clazz the class to compare
     * @param keyExtractor1 the first key to used for comparison
     * @param keyExtractor2 the second key to used for comparison
     * @return the builder
     */
    <T, U extends Comparable<? super U>> DeterministicObjectMapperBuilder withConverter(final Class<T> clazz,
        final Function<? super T, ? extends U> keyExtractor1, final Function<? super T, ? extends U> keyExtractor2) {
      final Comparator<T> comparator1 = comparingNullable(keyExtractor1);
      final Comparator<T> comparator2 = comparingNullable(keyExtractor2);
      return withConverter(clazz, comparator1.thenComparing(comparator2));
    }

    /**
     * Adds a custom converter for the given class, using the given {@link Comparator}.
     *
     * @param clazz the class to compare
     * @param comparator the comparator used for comparison
     * @return the builder
     */
    <T> DeterministicObjectMapperBuilder withConverter(final Class<T> clazz, final Comparator<T> comparator) {
      customComparators.addConverter(clazz, comparator);
      return this;
    }

    /**
     * @return the configured {@link DeterministicObjectMapper} instance
     */
    ObjectMapper build() {
      final ObjectMapper mapper = JsonMapper.builder(new YAMLFactory()
          .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
          .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
          .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .withConfigOverride(Map.class, configOverride -> configOverride.setInclude(construct(NON_NULL, NON_NULL)))
          .serializationInclusion(NON_EMPTY)
          .build();

      // this module is responsible for replacing non-deterministic objects with deterministic ones
      // (for example, convert a Set to a sorted List)
      final SimpleModule module = new SimpleModule();
      module.addSerializer(Collection.class, new SortedCollectionDelegatingSerializerProvider(mapper, customComparators));
      mapper.registerModule(module);
      return mapper;
    }

    private static <T, U extends Comparable<? super U>> Comparator<T> comparingNullable(
        final Function<? super T, ? extends U> keyExtractor) {
      Objects.requireNonNull(keyExtractor);
      return (Comparator<T> & Serializable) (c1, c2) -> {
        U value1 = keyExtractor.apply(c1);
        U value2 = keyExtractor.apply(c2);
        if (value1 == null || value2 == null) {
          return 0;
        }
        return value1.compareTo(value2);
      };
    }
  }

  private static class CustomComparators {
    private final Map<Class<?>, Comparator<?>> customComparators = new ConcurrentHashMap<>();

    private <T> void addConverter(final Class<T> clazz, final Comparator<T> comparator) {
      customComparators.put(clazz, comparator);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int compare(final Object first, final Object second) {
      if (first instanceof Comparable) {
        return ((Comparable) first).compareTo(second);
      }

      // try custom supplied comparators
      final Class<?> firstClass = first.getClass();
      for (Map.Entry<Class<?>, Comparator<?>> entry : customComparators.entrySet()) {
        final Class<?> clazz = entry.getKey();
        if (firstClass.isAssignableFrom(clazz)) {
          final Comparator<Object> comparator = (Comparator<Object>) entry.getValue();
          return comparator.compare(first, second);
        }
      }

      // try reflection on getName() and getId()
      final Integer getNameResult = compareWithReflection(first, second, firstClass, "getName");
      if (getNameResult != null) {
        return getNameResult;
      }
      final Integer getIdResult = compareWithReflection(first, second, firstClass, "getId");
      if (getIdResult != null) {
        return getIdResult;
      }

      // give up
      LOGGER.warn("Could not sort instances of type {}, the CRD generation may not be deterministic", firstClass.getName());
      return 0;
    }

    private static Integer compareWithReflection(final Object first, final Object second, final Class<?> clazz,
        final String methodName) {
      try {
        final Method method = clazz.getMethod(methodName);
        if (method.getReturnType() == String.class) {
          final String firstValue = (String) method.invoke(first);
          final String secondValue = (String) method.invoke(second);
          if (firstValue == null || secondValue == null) {
            return 0;
          }
          LOGGER.info("Using reflection to compare instance of {}", clazz.getName());
          return firstValue.compareTo(secondValue);
        }
      } catch (NoSuchMethodException ignored) {
        // nothing to do in this case
      } catch (Exception e) {
        LOGGER.warn("Could not compare instances of type {} via reflection", clazz.getName(), e);
      }
      return null;
    }
  }

  /**
   * Sorts collections on serialization using the provided custom comparators.
   *
   * This serializer first converts collections to sorted lists, then delegates back to the original serializer to prevent
   * infinite loops and stack overflows.
   */
  private static class SortedCollectionDelegatingSerializerProvider extends StdDelegatingSerializer {
    private final SerializerProvider serializerProvider;

    private SortedCollectionDelegatingSerializerProvider(final ObjectMapper mapper, final CustomComparators customComparators) {
      super(new CollectionToSortedListConverter(customComparators));
      serializerProvider = mapper.getSerializerProviderInstance();
    }

    @Override
    protected StdDelegatingSerializer withDelegate(final Converter<Object, ?> converter,
        final JavaType delegateType, final JsonSerializer<?> delegateSerializer) {
      return new StdDelegatingSerializer(converter, delegateType, delegateSerializer);
    }

    @Override
    public JsonSerializer<?> createContextual(final SerializerProvider provider, final BeanProperty property)
        throws JsonMappingException {
      // delegate this call to the original SerializerProvider, because this method recursively calls itself
      return super.createContextual(serializerProvider, property);
    }
  }

  private static class CollectionToSortedListConverter extends StdConverter<Collection<?>, Collection<?>> {
    private final Comparator<Object> comparator;

    public CollectionToSortedListConverter(final CustomComparators customComparators) {
      // if a collection is heterogeneous or has anonymous classes it's better to sort by the class name first
      // (we prioritize a deterministic output over the actual sort order)
      this.comparator = Comparator.comparing(o -> o.getClass().getName()).thenComparing(customComparators::compare);
    }

    @Override
    public Collection<?> convert(final Collection<?> value) {
      if (value == null || value.isEmpty()) {
        return Collections.emptyList();
      }
      return value.stream().filter(Objects::nonNull).sorted(comparator).collect(Collectors.toList());
    }
  }
}
