/*
 * Copyright 2017-2026 Alfresco Software, Ltd.
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
 * Process definition registry for security policy tests
 * Maps process names to their actual process definition keys
 */
export class ProcessDefinitionRegistry {
    private static readonly processWithTasksDefinitionKeys = new Map<string, string>([
        ['PROCESS_INSTANCE_WITH_VARIABLES', 'ProcessWithVariables'],
        ['TWO_TASK_PROCESS', 'twoTaskProcess'],
        ['PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', 'SingleTaskProcess'],
        ['PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES', 'SingleTaskProcessUserCandidates'],
        ['PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES', 'SingleTaskProcessGroupCandidates'],
        ['PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO', 'fixSystemFailure'],
        ['PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP', 'singletask-b6095889-6177-4b73-b3d9-316e47749a36'],
        ['SUB_PROCESS_INSTANCE_WITH_TASK', 'subprocess-970cb8df-2d4c-482b-a7f8-c19a983c2ef2'],
        ['PROCESS_WITH_HEADERS_CONNECTOR', 'HeadersConnectorProcess'],
    ]);

    private static readonly processWithNoTasksDefinitionKeys = new Map<string, string>([
        ['SIMPLE_PROCESS_INSTANCE', 'SimpleProcess'],
        ['CONNECTOR_PROCESS_INSTANCE', 'ConnectorProcess'],
        ['CONNECTOR_PROCESS_WITH_LOOP', 'ConnectorProcessWithLoop'],
        ['BPMN_ERROR_CONNECTOR_PROCESS', 'testBpmnErrorConnectorProcess'],
        ['ERROR_CONNECTOR_REPLAY_PROCESS', 'testErrorConnectorProcess'],
        ['PROCESS_INSTANCE_WITH_CALL_ACTIVITIES', 'parentproc-8e992556-5785-4ee0-9fe7-354decfea4a8'],
        ['Process Information', 'processinf-4e42752c-cc4d-429b-9528-7d3df24a9537'],
        ['Process with Generic BPMN Task', 'processwit-c6fd1b26-0d64-47f2-8d04-0b70764444a7']
    ]);

    public static readonly processDefinitionKeys = new Map<string, string>([
        ...ProcessDefinitionRegistry.processWithTasksDefinitionKeys,
        ...ProcessDefinitionRegistry.processWithNoTasksDefinitionKeys
    ]);

    public static processDefinitionKeyMatcher(processName: string): string {
        const key = ProcessDefinitionRegistry.processDefinitionKeys.get(processName);
        if (!key) {
            throw new Error(`Unknown process name: ${processName}`);
        }
        return key;
    }

    public static withTasks(processName: string): boolean {
        return ProcessDefinitionRegistry.processWithTasksDefinitionKeys.has(processName);
    }

    public static getProcessDefinitionKey(processName: string): string {
        return ProcessDefinitionRegistry.processDefinitionKeyMatcher(processName);
    }

    /** Symbolic process names — process-instance-actions.story parity. */
    public static readonly processInstanceActionsProcessNames = [
        'SIMPLE_PROCESS_INSTANCE',
        'PROCESS_INSTANCE_WITH_VARIABLES',
        'CONNECTOR_PROCESS_INSTANCE',
        'PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO',
        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
        'PROCESS_WITH_HEADERS_CONNECTOR',
    ] as const;

    /** Symbolic process names — task-actions.story wave 1 parity. */
    public static readonly taskActionsWave1ProcessNames = [
        'PROCESS_INSTANCE_WITH_VARIABLES',
        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
        'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES',
        'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES',
        'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP',
    ] as const;

    /** Symbolic process names — process-instance-service-tasks-actions.story parity. */
    public static readonly serviceTaskActionsProcessNames = [
        'CONNECTOR_PROCESS_INSTANCE',
        'CONNECTOR_PROCESS_WITH_LOOP',
        'BPMN_ERROR_CONNECTOR_PROCESS',
        'ERROR_CONNECTOR_REPLAY_PROCESS',
    ] as const;

    /** Symbolic process names — task-actions.story wave 2 (additional BPMN keys). */
    public static readonly taskActionsWave2ProcessNames = [
        'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES',
        'TWO_TASK_PROCESS',
    ] as const;

    public static definitionKeysForProcessNames(processNames: readonly string[]): string[] {
        return [...new Set(processNames.map((name) => ProcessDefinitionRegistry.getProcessDefinitionKey(name)))];
    }
}
