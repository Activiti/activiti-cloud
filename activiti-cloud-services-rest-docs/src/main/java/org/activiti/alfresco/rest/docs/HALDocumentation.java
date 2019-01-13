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
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
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

    public static LinksSnippet pageLinks() {
        return links(selfLink(),
              linkWithRel("last")
                      .description("The last page")
                      .optional(),
              linkWithRel("prev")
                      .description("The previous page")
                      .optional(),
              linkWithRel("first")
                      .description("The first page")
                      .optional(),
              linkWithRel("next")
                      .description("The next page")
                      .optional()

        );
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

    public static ResponseFieldsSnippet pagedProcessInstanceFields() {
        return pagedResponseFields(
                subsectionWithPath("_embedded.processInstances")
                        .description("List of process instances"),
                subsectionWithPath("_embedded.processInstances.[].id")
                        .description("The process instance id"),
                subsectionWithPath("_embedded.processInstances.[].processDefinitionId")
                        .description("The id of related process definition"),
                subsectionWithPath("_embedded.processInstances.[].status")
                        .description("The process instance status"),
                subsectionWithPath("_embedded.processInstances.[].name")
                        .description("The process instance name")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].serviceName")
                        .description("The name of the service where this process instance comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].serviceFullName")
                        .description("The full name of the service where this process instance comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].serviceVersion")
                        .description("The version of the service where this process instance comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].serviceType")
                        .description("The type of the service where this process instance comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].appName")
                        .description("The name of the application where this process instance comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.processInstances.[].appVersion")
                        .description("The version of the application where this process instance comes from")
                        .type(String.class)
                        .optional()
        );
    }

    public static ResponseFieldsSnippet pagedTasksFields() {
        return pagedResponseFields(
                subsectionWithPath("_embedded.tasks")
                        .description("List of tasks"),
                subsectionWithPath("_embedded.tasks.[].id")
                        .description("The task id"),
                subsectionWithPath("_embedded.tasks.[].processDefinitionId")
                        .description("The id of related process definition")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].processInstanceId")
                        .description("The id of related process instance")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].assignee")
                        .description("The task assignee")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].description")
                        .description("The task description")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].createdDate")
                        .description("The date where the task has been created"),
                subsectionWithPath("_embedded.tasks.[].dueDate")
                        .description("The date where the task is due")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].claimedDate")
                        .description("The date where the task has been claimed")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].priority")
                        .description("The task priority"),
                subsectionWithPath("_embedded.tasks.[].status")
                        .description("The task status"),
                subsectionWithPath("_embedded.tasks.[].name")
                        .description("The task name"),
                subsectionWithPath("_embedded.tasks.[].serviceName")
                        .description("The name of the service where this task comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].serviceFullName")
                        .description("The full name of the service where this task comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].serviceVersion")
                        .description("The version of the service where this task comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].serviceType")
                        .description("The type of the service where this task comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].appName")
                        .description("The name of the application where this task comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.tasks.[].appVersion")
                        .description("The version of the application where this task comes from")
                        .type(String.class)
                        .optional()
        );
    }

    public static ResponseFieldsSnippet pagedVariablesFields() {
        return pagedResponseFields(
                subsectionWithPath("_embedded.variables")
                        .description("List of variables"),
                subsectionWithPath("_embedded.variables.[].processInstanceId")
                        .description("The id of related process instance"),
                subsectionWithPath("_embedded.variables.[].taskId")
                        .description("The id of related task")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].name")
                        .description("The variable name"),
                subsectionWithPath("_embedded.variables.[].value")
                        .description("The variable value"),
                subsectionWithPath("_embedded.variables.[].type")
                        .description("The variable type"),
                subsectionWithPath("_embedded.variables.[].serviceName")
                        .description("The name of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceFullName")
                        .description("The full name of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceVersion")
                        .description("The version of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceType")
                        .description("The type of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].appName")
                        .description("The name of the application where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].appVersion")
                        .description("The version of the application where this variable comes from")
                        .type(String.class)
                        .optional()
        );
    }

    public static ResponseFieldsSnippet unpagedVariableFields() {
        return responseFields(
                subsectionWithPath("_embedded.variables")
                        .description("List of variables"),
                subsectionWithPath("_embedded.variables.[].processInstanceId")
                        .description("The id of related process instance"),
                subsectionWithPath("_embedded.variables.[].taskId")
                        .description("The id of related task")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].name")
                        .description("The variable name"),
                subsectionWithPath("_embedded.variables.[].value")
                        .description("The variable value"),
                subsectionWithPath("_embedded.variables.[].type")
                        .description("The variable type"),
                subsectionWithPath("_embedded.variables.[].serviceName")
                        .description("The name of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceFullName")
                        .description("The full name of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceVersion")
                        .description("The version of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].serviceType")
                        .description("The type of the service where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].appName")
                        .description("The name of the application where this variable comes from")
                        .type(String.class)
                        .optional(),
                subsectionWithPath("_embedded.variables.[].appVersion")
                        .description("The version of the application where this variable comes from")
                        .type(String.class)
                        .optional()
        );
    }

}
