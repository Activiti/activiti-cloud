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

package org.activiti.alfresco.rest.docs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;

public class HALDocumentation {

    private static ResponseFieldsSnippet pagedResponseFields(FieldDescriptor... fieldDescriptors) {

        List<FieldDescriptor> allDescriptors = new ArrayList<>();
        allDescriptors.addAll(Arrays.asList(fieldDescriptors));
        allDescriptors.addAll(Arrays.asList(pagedResponseFieldDescriptors()));
        return responseFields(allDescriptors);
    }

    private static FieldDescriptor[] pagedResponseFieldDescriptors() {
        return new FieldDescriptor[]{
                    subsectionWithPath("_links")
                            .ignored(),
                    subsectionWithPath("page")
                            .description("The pagination metadata"),
                    subsectionWithPath("page.size")
                            .description("The page size"),
                    subsectionWithPath("page.totalElements")
                            .description("The total number of elements available"),
                    subsectionWithPath("page.totalPages")
                            .description("The number of pages available"),
                    subsectionWithPath("page.number")
                            .description("The current page number")};
    }

    public static LinkDescriptor selfLink() {
        return linkWithRel("self").description("URL called to get this result");
    }

    public static ResponseFieldsSnippet pagedProcessDefinitionFields() {
        return pagedResponseFields(
                subsectionWithPath("_embedded.processDefinitions")
                        .description("List of process definitions"),
                subsectionWithPath("_embedded.processDefinitions.[].id")
                        .description("The process definition id"),
                subsectionWithPath("_embedded.processDefinitions.[].name")
                        .description("The process definition name"),
                subsectionWithPath("_embedded.processDefinitions.[].key")
                        .description("The process definition key"),
                subsectionWithPath("_embedded.processDefinitions.[].version")
                        .description("The process definition version"),
                subsectionWithPath("_embedded.processDefinitions.[].description")
                        .description("The process definition description")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].formKey")
                        .description("The key of the form related to this process definition")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].serviceName")
                        .description("The name of the service where this process is deployed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].serviceFullName")
                        .description("The full name of the service where this process is deployed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].serviceVersion")
                        .description("The version of the service where this process is deployed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].serviceType")
                        .description("The type of the service where this process is deployed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].appName")
                        .description("The name of the application where this process is deployed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processDefinitions.[].appVersion")
                        .description("The version of the application where this process is deployed")
                        .type(String.class)
                        .optional()
        );
    }
}
