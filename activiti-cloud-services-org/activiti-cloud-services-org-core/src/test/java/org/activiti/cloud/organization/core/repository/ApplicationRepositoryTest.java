/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.organization.core.repository;

import org.activiti.cloud.organization.core.model.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ApplicationRepository}
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationRepositoryTest {

    @Spy
    private ApplicationRepository applicationRepository;

    @Captor
    private ArgumentCaptor<Application> applicationArgumentCaptor;

    @Test
    public void testCreateApplication() {
        // GIVEN
        Application applicationToCreate = new Application("new_application_id",
                                                   "New Application");

        // WHEN
        applicationRepository.createApplication(applicationToCreate);

        // THEN
        verify(applicationRepository,
               times(1))
                .createApplication(applicationArgumentCaptor.capture());

        Application application = applicationArgumentCaptor.getValue();
        assertThat(application).isNotNull();
        assertThat(application.getId()).isEqualTo("new_application_id");
        assertThat(application.getName()).isEqualTo("New Application");
    }

    @Test
    public void testUpdateApplication() {
        // GIVEN
        Application applcationToUpdate = new Application("application_id",
                                                      "Application Name");
        Application application = new Application("new_application_id",
                                              "New Application Name");

        // WHEN
        applicationRepository.updateApplication(applcationToUpdate,
                                                application);

        // THEN
        verify(applicationRepository,
               times(1))
                .updateApplication(applicationArgumentCaptor.capture());

        Application updatedApplication = applicationArgumentCaptor.getValue();
        assertThat(updatedApplication).isNotNull();
        assertThat(updatedApplication.getId()).isEqualTo("application_id");
        assertThat(updatedApplication.getName()).isEqualTo("New Application Name");
    }
}
