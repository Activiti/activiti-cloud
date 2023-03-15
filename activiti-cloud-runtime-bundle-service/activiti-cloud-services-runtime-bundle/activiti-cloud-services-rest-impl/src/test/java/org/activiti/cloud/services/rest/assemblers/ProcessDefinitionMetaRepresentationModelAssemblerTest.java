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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

import java.util.Optional;
import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

public class ProcessDefinitionMetaRepresentationModelAssemblerTest {

    private ProcessDefinitionMetaRepresentationModelAssembler representationModelAssembler = new ProcessDefinitionMetaRepresentationModelAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionMeta model = mock(ProcessDefinitionMeta.class);
        when(model.getId()).thenReturn("my-identifier");

        EntityModel<ProcessDefinitionMeta> resource = representationModelAssembler.toModel(model);

        Optional<Link> selfResourceLink = resource.getLink(SELF);

        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");

        Optional<Link> metaResourceLink = resource.getLink("meta");

        assertThat(metaResourceLink).isPresent();
        assertThat(metaResourceLink.get().getHref()).contains("my-identifier");
    }
}
