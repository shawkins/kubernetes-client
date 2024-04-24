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
package io.fabric8.crdv2.generator.v1;

import io.fabric8.crd.generator.annotation.PrinterColumn;
import io.fabric8.crdv2.generator.AbstractCustomResourceHandler;
import io.fabric8.crdv2.generator.CustomResourceInfo;
import io.fabric8.crdv2.generator.ResolvingContext;
import io.fabric8.crdv2.generator.Resources;
import io.fabric8.crdv2.generator.decorator.Decorator;
import io.fabric8.crdv2.generator.v1.decorator.AddAdditionPrinterColumnDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddCustomResourceDefinitionResourceDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddCustomResourceDefinitionVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddLabelSelectorPathDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddSchemaToCustomResourceDefinitionVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddSpecReplicasPathDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddStatusReplicasPathDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddStatusSubresourceDecorator;
import io.fabric8.crdv2.generator.v1.decorator.AddSubresourcesDecorator;
import io.fabric8.crdv2.generator.v1.decorator.EnsureSingleStorageVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.SetDeprecatedVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.SetServedVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.SetStorageVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.SortCustomResourceDefinitionVersionDecorator;
import io.fabric8.crdv2.generator.v1.decorator.SortPrinterColumnsDecorator;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.model.annotation.LabelSelector;
import io.fabric8.kubernetes.model.annotation.SpecReplicas;
import io.fabric8.kubernetes.model.annotation.StatusReplicas;

public class CustomResourceHandler extends AbstractCustomResourceHandler {

  public static final String VERSION = "v1";

  public CustomResourceHandler(Resources resources) {
    super(resources);
  }

  @Override
  protected Decorator<?> getPrinterColumnDecorator(String name,
      String version, String path,
      String type, String column, String description, String format, int priority) {
    return new AddAdditionPrinterColumnDecorator(name, version, type, column, path, format,
        description, priority);
  }

  @Override
  public void handle(CustomResourceInfo config) {
    final String name = config.crdName();
    final String version = config.version();
    resources.decorate(
        new AddCustomResourceDefinitionResourceDecorator(name, config.group(), config.kind(),
            config.scope().value(), config.shortNames(), config.plural(), config.singular(), config.annotations(),
            config.labels()));

    resources.decorate(new AddCustomResourceDefinitionVersionDecorator(name, version));

    JsonSchema resolver = new JsonSchema(ResolvingContext.defaultResolvingContext(), config.definition());
    JSONSchemaProps schema = resolver.getSchema();

    resources.decorate(new AddSchemaToCustomResourceDefinitionVersionDecorator(name, version,
        schema));

    resolver.getSinglePath(SpecReplicas.class).ifPresent(path -> {
      resources.decorate(new AddSubresourcesDecorator(name, version));
      resources.decorate(new AddSpecReplicasPathDecorator(name, version, path));
    });

    resolver.getSinglePath(StatusReplicas.class).ifPresent(path -> {
      resources.decorate(new AddSubresourcesDecorator(name, version));
      resources.decorate(new AddStatusReplicasPathDecorator(name, version, path));
    });

    resolver.getSinglePath(LabelSelector.class).ifPresent(path -> {
      resources.decorate(new AddSubresourcesDecorator(name, version));
      resources.decorate(new AddLabelSelectorPathDecorator(name, version, path));
    });

    handlePrinterColumns(name, version, resolver.getAllPaths(PrinterColumn.class));

    if (config.statusClassName().isPresent()) {
      resources.decorate(new AddSubresourcesDecorator(name, version));
      resources.decorate(new AddStatusSubresourceDecorator(name, version));
    }

    resources.decorate(new SetServedVersionDecorator(name, version, config.served()));
    resources.decorate(new SetStorageVersionDecorator(name, version, config.storage()));
    resources.decorate(new SetDeprecatedVersionDecorator(name, version, config.deprecated(), config.deprecationWarning()));
    resources.decorate(new EnsureSingleStorageVersionDecorator(name));
    resources.decorate(new SortCustomResourceDefinitionVersionDecorator(name));
    resources.decorate(new SortPrinterColumnsDecorator(name, version));
  }

}