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

package io.fabric8.crd.generator.keycloak;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

@Group("sample.fabric8.io")
@Version("v1alpha1")
@SchemaSwap(originalType = GroupRepresentation.class, fieldName = "subGroups", depth = 10)
@SchemaSwap(originalType = ComponentExportRepresentation.class, fieldName = "subComponents", depth = 10)
@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "policies")
@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "resources")
public class KeycloakRealm extends CustomResource<RealmRepresentation, Void> implements Namespaced {

}
