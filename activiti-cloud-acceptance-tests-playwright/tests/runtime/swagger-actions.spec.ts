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

/**
 * Ported from JBehave story:
 *   activiti-cloud-acceptance-scenarios/runtime-acceptance-tests/src/main/resources/stories/runtime-bundle/swagger-actions.story
 *
 * Original story preserved verbatim:
 *
 *   Scenario: retrieve the swagger specification
 *   Given the user is authenticated as testuser
 *   When the user asks for swagger specification
 *   Then the user gets swagger specification following Alfresco MediaType
 */

import { activiti, expect } from '../../fixtures/services.fixture';

activiti.describe('Runtime — Swagger Actions', () => {
    activiti(
        'retrieve the swagger specification',
        async ({ runtimeBundleServiceTestUser, queryServiceTestUser, auditServiceTestUser }) => {
            let runtimeBundleSwagger = '';
            let querySwagger = '';
            let auditSwagger = '';

            await activiti.step('When the user asks for swagger specification', async () => {
                runtimeBundleSwagger = await runtimeBundleServiceTestUser.getSwaggerSpecification();
                querySwagger = await queryServiceTestUser.openApiSpec.getSwaggerSpecification();
                auditSwagger = await auditServiceTestUser.events.getSwaggerSpecification();
            });

            await activiti.step(
                'Then the user gets swagger specification following Alfresco MediaType',
                async () => {
                    expect(runtimeBundleSwagger).toContain('ListResponseContentExtendedCloudProcessDefinition');
                    expect(runtimeBundleSwagger).toContain('EntryResponseContentCloudProcessDefinition');
                    expect(runtimeBundleSwagger).toContain('payloadType');
                    expect(runtimeBundleSwagger).not.toContain('PagedModel');
                    expect(runtimeBundleSwagger).not.toContain('ResourcesResource');
                    expect(runtimeBundleSwagger).not.toContain('"Resource"');

                    expect(querySwagger).toContain('ListResponseContentCloudProcessDefinition');
                    expect(querySwagger).toContain('EntriesResponseContentCloudProcessDefinition');
                    expect(querySwagger).toContain('EntryResponseContentCloudProcessDefinition');
                    expect(querySwagger).not.toContain('PagedModel');
                    expect(querySwagger).not.toContain('ResourcesResource');
                    expect(querySwagger).not.toContain('"Resource"');

                    expect(auditSwagger).toContain('ListResponseContentCloudRuntimeEventObjectCloudRuntimeEventType');
                    expect(auditSwagger).toContain('EntriesResponseContentCloudRuntimeEventObjectCloudRuntimeEventType');
                    expect(auditSwagger).toContain('EntryResponseContentCloudRuntimeEventObjectCloudRuntimeEventType');
                    expect(auditSwagger).toContain('CloudRuntimeEventModel');
                    expect(auditSwagger).not.toContain('PagedModel');
                    expect(auditSwagger).not.toContain('ResourcesResource');
                    expect(auditSwagger).not.toContain('"Resource"');
                    expect(auditSwagger).not.toContain('Enum');
                }
            );
        }
    );
});
