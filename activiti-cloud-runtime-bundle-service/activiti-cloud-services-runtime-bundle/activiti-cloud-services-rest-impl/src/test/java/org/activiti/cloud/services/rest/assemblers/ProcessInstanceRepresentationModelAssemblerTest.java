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
package org.activiti.cloud.services.rest.assemblers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

@ExtendWith(MockitoExtension.class)
public class ProcessInstanceRepresentationModelAssemblerTest {

    @InjectMocks
    private ProcessInstanceRepresentationModelAssembler representationModelAssembler;

    @Mock
    private ToCloudProcessInstanceConverter toCloudProcessInstanceConverter;

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        CloudProcessInstance cloudModel = mock(CloudProcessInstance.class);
        given(cloudModel.getId()).willReturn("my-identifier");

        ProcessInstance model = mock(ProcessInstance.class);
        given(toCloudProcessInstanceConverter.from(model)).willReturn(cloudModel);

        //when
        EntityModel<CloudProcessInstance> resource = representationModelAssembler.toModel(model);

        //then
        Optional<Link> selfResourceLink = resource.getLink(SELF);
        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");
    }
}
