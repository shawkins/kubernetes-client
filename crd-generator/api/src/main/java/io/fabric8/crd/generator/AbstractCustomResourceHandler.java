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
package io.fabric8.crd.generator;

import io.fabric8.crd.generator.decorator.Decorator;

/**
 * This class encapsulates the common behavior between v1beta1 and v1 CRD generation logic. The
 * intent is that each CRD spec version is implemented as a sub-class of this one.
 */
public abstract class AbstractCustomResourceHandler {

  protected final Resources resources;

  protected AbstractCustomResourceHandler(Resources resources) {
    this.resources = resources;
  }

  public abstract void handle(CustomResourceInfo config);

  /*Map<String, Property> additionalPrinterColumns = new HashMap<>(additionalPrinterColumnDetector.getProperties());
  additionalPrinterColumns.forEach((path, property) -> {
    Map<String, Object> parameters = property.getAnnotations().stream()
        .filter(a -> a.getClassRef().getName().equals("PrinterColumn")).map(AnnotationRef::getParameters)
        .findFirst().orElse(Collections.emptyMap());
    String type = AbstractJsonSchema.getSchemaTypeFor(property.getTypeRef());
    String column = (String) parameters.get("name");
    if (Utils.isNullOrEmpty(column)) {
      column = property.getName().toUpperCase();
    }
    String description = property.getComments().stream().filter(l -> !l.trim().startsWith("@"))
        .collect(Collectors.joining(" ")).trim();
    String format = (String) parameters.get("format");
    int priority = (int) parameters.getOrDefault("priority", 0);

    resources.decorate(
        getPrinterColumnDecorator(name, version, path, type, column, description, format, priority));
  });*/

  /**
   * Provides the decorator implementation associated with the CRD generation version.
   *
   * @param name the resource name
   * @param version the associated version
   * @param path the path from which the printer column is extracted
   * @param type the data type of the printer column
   * @param column the name of the column
   * @param description the description of the column
   * @param format the format of the printer column
   * @return the concrete decorator implementing the addition of a printer column to the currently built CRD
   */
  protected abstract Decorator<?> getPrinterColumnDecorator(String name, String version, String path,
      String type, String column, String description, String format, int priority);

}
