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
package org.activiti.cloud.services.query.app.repository;

/**
 * Projection interface for native query results that return
 * the top variable groups by storage consumption.
 */
public interface StorageConsumerProjection {
    String getSourceTable();

    String getProcessDefinitionKey();

    String getProcessDefinitionName();

    Integer getProcessDefinitionVersion();

    String getVariableName();

    String getVariableType();

    Long getInstanceCount();

    Long getTotalSizeBytes();

    Long getAvgSizeBytes();
}
