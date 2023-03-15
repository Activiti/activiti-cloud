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

import java.util.Map;
import org.activiti.cloud.services.metadata.MetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:application-test.properties")
public class MetadataServiceIT {

    @Autowired
    private MetadataService metadataService;

    @Test
    public void shouldGetMetaData() throws Exception {
        Map<String, String> metaData = metadataService.getMetadata();

        assertThat(metaData.keySet()).hasSize(5);
        assertThat(metaData.keySet()).contains("activiti-cloud-service-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-short-name");

        assertThat(metaData.values()).contains("1");
        assertThat(metaData.values()).contains("app");
    }
}
