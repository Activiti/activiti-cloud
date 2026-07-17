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
package org.activiti.cloud.services.query.events.handlers;

/**
 * Manages the {@code process_instance_hierarchy} closure table so that
 * hierarchy queries at any depth can be answered with a single JOIN.
 *
 * @see ProcessInstanceHierarchyServiceImpl
 */
public interface ProcessInstanceHierarchyService {
    /** Registers a brand-new root process (no parent, no linked). */
    void registerProcess(String processId);

    /** Registers a subprocess: inserts self-ref and propagates all ancestors of {@code parentId}. */
    void registerSubprocess(String processId, String parentId);

    /** Registers a linked-process relationship and propagates all ancestors of {@code linkedProcessInstanceId}. */
    void registerLinkedProcess(String processId, String linkedProcessInstanceId);

    /** Removes every hierarchy row that mentions {@code processId}. */
    void removeProcess(String processId);
}
