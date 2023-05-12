/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.UUID;
import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.api.process.model.events.CloudApplicationDeployedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.listeners.CleanUpDatabaseTestExecutionListener;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = { CleanUpDatabaseTestExecutionListener.class }
)
@ResourceLocks({ @ResourceLock(value = Resources.CHANNEL_BINDER, mode = ResourceAccessMode.READ_WRITE) })
public class QueryApplicationEntityIT {

    private static final String APPS_URL = "/v1/applications";
    private static final String ADMIN_APPS_URL = "/admin/v1/applications";

    private static final ParameterizedTypeReference<PagedModel<ApplicationEntity>> PAGED_APPLICATION_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ApplicationEntity>>() {};

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MyProducer producer;

    @AfterEach
    public void tearDown() {
        applicationRepository.deleteAll();
    }

    @Test
    public void shouldGetDeployedApplications() {
        getDeployedApplication(APPS_URL);
    }

    @Test
    public void shouldGetDeployedApplicationsWhenUserIsAdmin() {
        identityTokenProducer.withTestUser("hradmin");
        getDeployedApplication(ADMIN_APPS_URL);
    }

    private void getDeployedApplication(String url) {
        CloudApplicationDeployedEvent applicationDeployed1 = buildCloudApplicationDeployedEvent(
            "deployment1",
            "appName",
            1
        );
        CloudApplicationDeployedEvent applicationDeployed2 = buildCloudApplicationDeployedEvent(
            "deployment2",
            "appName",
            2
        );
        CloudApplicationDeployedEvent applicationDeployedDuplicated = buildCloudApplicationDeployedEvent(
            "deployment3",
            "appName",
            1
        );
        producer.send(applicationDeployed1, applicationDeployed2, applicationDeployedDuplicated);

        await()
            .untilAsserted(() -> {
                assertThat(applicationRepository.findAll()).hasSize(2);
            });

        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<ApplicationEntity>> responseEntity = testRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_APPLICATION_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ApplicationEntity> applicationEntities = responseEntity.getBody().getContent();
                assertThat(applicationEntities)
                    .extracting(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getVersion)
                    .contains(
                        tuple(
                            applicationDeployed1.getEntity().getId(),
                            applicationDeployed1.getAppName(),
                            applicationDeployed1.getEntity().getVersion().toString()
                        ),
                        tuple(
                            applicationDeployed2.getEntity().getId(),
                            applicationDeployed2.getAppName(),
                            applicationDeployed2.getEntity().getVersion().toString()
                        )
                    );
            });
    }

    @Test
    public void shouldGetDeployedApplicationFilteredByName() {
        String appToFilter = "appName";
        CloudApplicationDeployedEvent applicationDeployed1 = buildCloudApplicationDeployedEvent(
            "deployment1",
            appToFilter,
            1
        );
        CloudApplicationDeployedEvent applicationDeployed2 = buildCloudApplicationDeployedEvent(
            "deployment2",
            appToFilter,
            2
        );
        CloudApplicationDeployedEvent applicationDeployedDuplicated = buildCloudApplicationDeployedEvent(
            "deployment3",
            "appName_test",
            1
        );
        producer.send(applicationDeployed1, applicationDeployed2, applicationDeployedDuplicated);

        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<ApplicationEntity>> responseEntity = testRestTemplate.exchange(
                    APPS_URL + "?name=" + appToFilter,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_APPLICATION_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<ApplicationEntity> applicationEntities = responseEntity.getBody().getContent();
                assertThat(applicationEntities).extracting(ApplicationEntity::getName).containsOnly(appToFilter);
            });
    }

    private CloudApplicationDeployedEvent buildCloudApplicationDeployedEvent(String id, String name, int version) {
        DeploymentImpl deployment = new DeploymentImpl();
        deployment.setId(id);
        deployment.setName(name);
        deployment.setVersion(version);

        CloudApplicationDeployedEventImpl cloudApplicationDeployedEventImpl = new CloudApplicationDeployedEventImpl(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            deployment,
            ApplicationEvents.APPLICATION_DEPLOYED
        );
        cloudApplicationDeployedEventImpl.setAppName(name);
        return cloudApplicationDeployedEventImpl;
    }
}
