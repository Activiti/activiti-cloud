/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

import java.util.Optional;
import org.activiti.cloud.api.process.model.ProcessInstanceSearchResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

class ProcessInstanceSearchResultRepresentationModelAssemblerTest {

    private final ProcessInstanceSearchResultRepresentationModelAssembler assembler = new ProcessInstanceSearchResultRepresentationModelAssembler();

    @Nested
    class ToModel {

        @Test
        void should_buildSelfLinkContainingResourceId() {
            ProcessInstanceSearchResult dto = mock(ProcessInstanceSearchResult.class);
            given(dto.getId()).willReturn("pi-1");

            EntityModel<ProcessInstanceSearchResult> resource = assembler.toModel(dto);

            Optional<Link> selfLink = resource.getLink(SELF);
            assertThat(selfLink).isPresent();
            assertThat(selfLink.get().getHref()).contains("pi-1");
        }

        @Test
        void should_buildTasksLink() {
            ProcessInstanceSearchResult dto = mock(ProcessInstanceSearchResult.class);
            given(dto.getId()).willReturn("pi-1");

            EntityModel<ProcessInstanceSearchResult> resource = assembler.toModel(dto);

            Optional<Link> tasksLink = resource.getLink("tasks");
            assertThat(tasksLink).isPresent();
            assertThat(tasksLink.get().getHref()).contains("pi-1");
        }

        @Test
        void should_buildVariablesLink() {
            ProcessInstanceSearchResult dto = mock(ProcessInstanceSearchResult.class);
            given(dto.getId()).willReturn("pi-1");

            EntityModel<ProcessInstanceSearchResult> resource = assembler.toModel(dto);

            Optional<Link> variablesLink = resource.getLink("variables");
            assertThat(variablesLink).isPresent();
            assertThat(variablesLink.get().getHref()).contains("pi-1");
        }

        @Test
        void should_wrapEntityIntoEntityModel() {
            ProcessInstanceSearchResult dto = mock(ProcessInstanceSearchResult.class);
            given(dto.getId()).willReturn("pi-1");

            EntityModel<ProcessInstanceSearchResult> resource = assembler.toModel(dto);

            assertThat(resource.getContent()).isSameAs(dto);
        }
    }
}
