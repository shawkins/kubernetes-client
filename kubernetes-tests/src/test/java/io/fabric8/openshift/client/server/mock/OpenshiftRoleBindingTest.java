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
package io.fabric8.openshift.client.server.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.RoleBindingBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableKubernetesMockClient
class OpenshiftRoleBindingTest {

  KubernetesMockServer server;
  OpenShiftClient client;

  private RoleBinding expectedRoleBinding = new RoleBindingBuilder()
      .withNewMetadata()
      .withName("testrb")
      .endMetadata()
      .addNewSubject()
      .withKind("User")
      .withName("testuser1")
      .endSubject()
      .addNewSubject()
      .withKind("User")
      .withName("testuser2")
      .endSubject()
      .addNewSubject()
      .withKind("ServiceAccount")
      .withName("svcacct")
      .endSubject()
      .addNewSubject()
      .withKind("Group")
      .withName("testgroup")
      .endSubject()
      .build();

  @Test
  void testCreateWithOnlySubjects() throws Exception {
    server.expect()
        .post()
        .withPath("/apis/authorization.openshift.io/v1/namespaces/test/rolebindings")
        .andReturn(201, expectedRoleBinding)
        .once();

    RoleBinding response = client.roleBindings()
        .resource(
            new RoleBindingBuilder()
                .withNewMetadata()
                .withName("testrb")
                .endMetadata()
                .addNewSubject()
                .withKind("User")
                .withName("testuser1")
                .endSubject()
                .addNewSubject()
                .withKind("User")
                .withName("testuser2")
                .endSubject()
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName("svcacct")
                .endSubject()
                .addNewSubject()
                .withKind("Group")
                .withName("testgroup")
                .endSubject()
                .build())
        .create();
    assertEquals(expectedRoleBinding, response);

    assertEquals(expectedRoleBinding,
        new ObjectMapper().readerFor(RoleBinding.class).readValue(server.getLastRequest().getBody().readByteArray()));
  }

  @Test
  void testReplaceWithOnlySubjects() throws Exception {
    server.expect()
        .get()
        .withPath("/apis/authorization.openshift.io/v1/namespaces/test/rolebindings/testrb")
        .andReturn(200, expectedRoleBinding)
        .once();
    server.expect()
        .put()
        .withPath("/apis/authorization.openshift.io/v1/namespaces/test/rolebindings/testrb")
        .andReturn(200, expectedRoleBinding)
        .once();

    RoleBinding response = client.roleBindings()
        .resource(
            new RoleBindingBuilder()
                .withNewMetadata()
                .withName("testrb")
                .endMetadata()
                .addNewSubject()
                .withKind("User")
                .withName("testuser1")
                .endSubject()
                .addNewSubject()
                .withKind("User")
                .withName("testuser2")
                .endSubject()
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName("svcacct")
                .endSubject()
                .addNewSubject()
                .withKind("Group")
                .withName("testgroup")
                .endSubject()
                .build())
        .replace();
    assertEquals(expectedRoleBinding, response);

    assertEquals(expectedRoleBinding,
        new ObjectMapper().readerFor(RoleBinding.class).readValue(server.getLastRequest().getBody().inputStream()));
  }

}
