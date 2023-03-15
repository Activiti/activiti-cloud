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
package org.activit.cloud.services.metadata.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.services.metadata.MetadataProperties;
import org.activiti.cloud.services.metadata.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MetadataServiceTest {

    @InjectMocks
    private MetadataService metadataService;

    @Mock
    private MetadataProperties metadataProperties;

    @BeforeEach
    public void setUp() {
        HashMap<String, String> application = new HashMap<>();
        application.put("name", "app");
        application.put("version", "1");

        HashMap<String, String> service = new HashMap<>();
        service.put("name", "rb");
        service.put("version", "2");

        when(metadataProperties.getApplication()).thenReturn(application);
        when(metadataProperties.getService()).thenReturn(service);
    }

    @Test
    public void shouldGetMetaData() {
        Map<String, String> metaData = metadataService.getMetadata();

        assertThat(metaData.keySet()).hasSize(4);
        assertThat(metaData.keySet()).contains("activiti-cloud-service-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-version");

        assertThat(metaData.values()).contains("1");
        assertThat(metaData.values()).contains("app");
    }
}
