/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.audit.tests.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuditSwaggerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void defaultSpecificationFileShouldBeAlfrescoFormat() {
        //when
        ResponseEntity<Object> apiDoc = testRestTemplate.getForEntity("/v2/api-docs",
                                                                      Object.class);


        //then
        assertThat(apiDoc.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiDoc.getBody()).isNotNull();
        assertThat(apiDoc.getBody().toString())
                .contains("ListResponseContent«CloudRuntimeEvent»")
                .contains("EntriesResponseContent«CloudRuntimeEvent»")
                .contains("EntryResponseContent«CloudRuntimeEvent»")
                .doesNotContain("PagedResources«")
                .doesNotContain("Resources«Resource«")
                .doesNotContain("Resource«");
    }

}
