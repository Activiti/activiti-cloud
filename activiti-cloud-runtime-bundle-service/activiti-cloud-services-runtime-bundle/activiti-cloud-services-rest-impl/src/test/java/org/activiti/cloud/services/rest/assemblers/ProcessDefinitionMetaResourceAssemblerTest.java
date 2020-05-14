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

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessDefinitionMetaResourceAssemblerTest {

    private ProcessDefinitionMetaResourceAssembler resourceAssembler = new ProcessDefinitionMetaResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionMeta model = mock(ProcessDefinitionMeta.class);
        when(model.getId()).thenReturn("my-identifier");

        Resource<ProcessDefinitionMeta> resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");

        Link metaResourceLink = resource.getLink("meta");

        assertThat(metaResourceLink).isNotNull();
        assertThat(metaResourceLink.getHref()).contains("my-identifier");
    }
}
