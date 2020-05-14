/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessInstanceResourceAssemblerTest {

    @InjectMocks
    private ProcessInstanceResourceAssembler resourceAssembler;

    @Mock
    private ToCloudProcessInstanceConverter toCloudProcessInstanceConverter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        CloudProcessInstance cloudModel = mock(CloudProcessInstance.class);
        given(cloudModel.getId()).willReturn("my-identifier");

        ProcessInstance model = mock(ProcessInstance.class);
        given(toCloudProcessInstanceConverter.from(model)).willReturn(cloudModel);

        //when
        Resource<CloudProcessInstance> resource = resourceAssembler.toResource(model);

        //then
        Link selfResourceLink = resource.getLink("self");
        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
