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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.model.shared.impl.CloudVariableInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskVariablesResourceAssemblerTest {

    @InjectMocks
    private TaskVariableInstanceResourceAssembler resourceAssembler;

    @Mock
    private ToCloudVariableInstanceConverter converter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        VariableInstance model = new VariableInstanceImpl<>("var", "string", "value", "procInstId");
        ((VariableInstanceImpl) model).setTaskId("my-identifier");

        given(converter.from(model)).willReturn(new CloudVariableInstanceImpl<>(model));

        //when
        Resource<CloudVariableInstance> resource = resourceAssembler.toResource(model);

        //then
        Link globalVariablesLink = resource.getLink("variables");

        assertThat(globalVariablesLink).isNotNull();
        assertThat(globalVariablesLink.getHref()).contains("my-identifier");
    }
}
