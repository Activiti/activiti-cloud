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
package org.activiti.cloud.services.query.events.handlers;

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.events.ApplicationEvent;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationDeployedEventHandlerTest {

    private static final String APPLICATION_DEPLOYMENT_NAME = "SpringAutoDeployment";

    @InjectMocks
    private ApplicationDeployedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationRepository applicationRepository;

    @Test
    public void handleShouldStoreApplication() {
        //given
        DeploymentImpl deployment = new DeploymentImpl();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setName(APPLICATION_DEPLOYMENT_NAME);
        deployment.setVersion(2);

        CloudApplicationDeployedEventImpl applicationDeployedEvent = new CloudApplicationDeployedEventImpl(deployment);
        applicationDeployedEvent.setAppName("ApplicationEventName");

        //when
        handler.handle(applicationDeployedEvent);

        //then
        ArgumentCaptor<ApplicationEntity> applicationCaptor = ArgumentCaptor.forClass(ApplicationEntity.class);

        verify(entityManager).persist(applicationCaptor.capture());
        ApplicationEntity application = applicationCaptor.getValue();
        assertThat(application)
            .hasId(deployment.getId())
            .hasName(applicationDeployedEvent.getAppName())
            .hasVersion(deployment.getVersion().toString());
    }

    @Test
    public void handleShouldNotStoreApplicationWhenAlreadyExist() {
        //given
        DeploymentImpl deployment = new DeploymentImpl();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setName(APPLICATION_DEPLOYMENT_NAME);
        deployment.setVersion(2);

        CloudApplicationDeployedEventImpl applicationDeployedFirstEvent = new CloudApplicationDeployedEventImpl(
            deployment
        );
        applicationDeployedFirstEvent.setAppName("ApplicationEventName");
        given(applicationRepository.existsByNameAndVersion(any(), any())).willReturn(true);

        //when
        handler.handle(applicationDeployedFirstEvent);

        //then
        verify(entityManager, never()).persist(any());
    }

    @Test
    public void getHandledEventShouldReturnApplicationDeployedEvent() {
        String handledEvent = handler.getHandledEvent();

        Assertions.assertThat(handledEvent).isEqualTo(ApplicationEvent.ApplicationEvents.APPLICATION_DEPLOYED.name());
    }
}
