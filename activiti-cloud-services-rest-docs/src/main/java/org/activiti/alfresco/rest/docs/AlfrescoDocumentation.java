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

import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.request.RequestParametersSnippet;

import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;

public class AlfrescoDocumentation {

    private AlfrescoDocumentation() {
    }

    public static RequestParametersSnippet pageRequestParameters() {
        return requestParameters(
                parameterWithName("skipCount")
                        .description("How many entities exist in the entire addressed collection before those included in this list."),
                parameterWithName("maxItems")
                        .description("The max number of entities that can be included in the result.")
        );
    }

    public static ResponseFieldsSnippet pagedResourcesResponseFields() {
        return responseFields(
                subsectionWithPath("list").ignored(),
                subsectionWithPath("list.entries").description("List of results."),
                subsectionWithPath("list.entries[].entry").description("Wrapper for each entry in the list of results."),
                subsectionWithPath("list.pagination").description("Pagination metadata."),
                subsectionWithPath("list.pagination.skipCount")
                        .description("How many entities exist in the entire addressed collection before those included in this list."),
                subsectionWithPath("list.pagination.maxItems")
                        .description("The maxItems parameter used to generate this list."),
                subsectionWithPath("list.pagination.count")
                        .description("The number of entities included in this list. This number must correspond to the number of objects in the \"entries\" array."),
                subsectionWithPath("list.pagination.hasMoreItems")
                        .description("A boolean value that indicates whether there are further entities in the addressed collection beyond those returned " +
                                             "in this response. If true then a request with a larger value for either the skipCount or the maxItems " +
                                             "parameter is expected to return further results."),
                subsectionWithPath("list.pagination.totalItems")
                        .description("An integer value that indicates the total number of entities in the addressed collection.")
        );
    }

    public static ResponseFieldsSnippet processDefinitionFields() {
        return responseFields(
                subsectionWithPath("entry").ignored(),
                subsectionWithPath("entry.id").description("The process definition id."),
                subsectionWithPath("entry.name").description("The process definition name."),
                subsectionWithPath("entry.description").description("The process definition description."),
                subsectionWithPath("entry.version").description("The process definition version.")
        );
    }

    public static PathParametersSnippet processDefinitionIdParameter() {
        return idParameter("The process definition id.");
    }

    private static PathParametersSnippet idParameter(String description) {
        return pathParameters(parameterWithName("id").description(description));
    }

    public static PathParametersSnippet processInstanceIdParameter() {
        return pathParameters(parameterWithName("processInstanceId").description("The process instance id."));
    }

    public static PathParametersSnippet taskIdParameter() {
        return pathParameters(parameterWithName("taskId").description("The task id."));
    }

    public static PathParametersSnippet variableIdParameter() {
        return pathParameters(parameterWithName("variableId").description("The variable id."));
    }

    public static ResponseFieldsSnippet processInstanceFields() {
        return responseFields(
                subsectionWithPath("entry").ignored(),
                subsectionWithPath("entry.id").description("The process instance id."),
                subsectionWithPath("entry.applicationName")
                        .description("The name of the application which started this process instance."),
                subsectionWithPath("entry.processDefinitionId").description("The related process definition id."),
                subsectionWithPath("entry.status").description("The process instance status."),
                subsectionWithPath("entry.lastModified").description("The process instance last modified date.")
        );
    }

    public static ResponseFieldsSnippet taskFields() {
        return responseFields(
                subsectionWithPath("entry").ignored(),
                subsectionWithPath("entry.id").description("The process instance id."),
                subsectionWithPath("entry.assignee").description("The use assigned to this task."),
                subsectionWithPath("entry.name").description("The task name."),
                subsectionWithPath("entry.description").description("The task description."),
                subsectionWithPath("entry.createTime").description("Time where this task has been created."),
                subsectionWithPath("entry.dueDate").description("Date this task is due for."),
                subsectionWithPath("entry.priority").description("The task priority."),
                subsectionWithPath("entry.category").description("The task category."),
                subsectionWithPath("entry.processDefinitionId").description("The related process definition id."),
                subsectionWithPath("entry.processInstanceId").description("The related process instance id."),
                subsectionWithPath("entry.applicationName")
                        .description("The name of the application which started this process instance."),
                subsectionWithPath("entry.status").description("The process instance status."),
                subsectionWithPath("entry.lastModified").description("The process instance last modified date."),
                subsectionWithPath("entry.claimDate").description("The date where this task was claimed.")
        );
    }

    public static ResponseFieldsSnippet variableFields() {
        return responseFields(
                subsectionWithPath("entry").ignored(),
                subsectionWithPath("entry.id").description("The process instance id."),
                subsectionWithPath("entry.type").description("The variable type."),
                subsectionWithPath("entry.name").description("The variable type."),
                subsectionWithPath("entry.processInstanceId").description("The related process instance id."),
                subsectionWithPath("entry.applicationName")
                        .description("The name of the application which started this process instance."),
                subsectionWithPath("entry.taskId").description("The related task id."),
                subsectionWithPath("entry.createTime").description("Time where this variable has been created."),
                subsectionWithPath("entry.lastUpdatedTime").description("The variable last modified date."),
                subsectionWithPath("entry.value").description("The variable value.")
        );
    }

}
