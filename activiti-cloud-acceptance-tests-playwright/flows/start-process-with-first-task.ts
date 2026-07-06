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

import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudTask } from '../models/task.models';
import { RuntimeBundleService } from '../services/runtime-bundle/runtime-bundle.service';
import { TaskService } from '../services/task/task.service';

export interface StartCatalogProcessOptions {
    name?: string;
    businessKey?: string;
    variables?: Record<string, unknown>;
}

export interface ProcessWithFirstTask {
    processInstance: CloudProcessInstance;
    task: CloudTask;
}

export async function startCatalogProcess(
    runtime: RuntimeBundleService,
    catalogProcessName: string,
    options: StartCatalogProcessOptions = {}
): Promise<CloudProcessInstance> {
    const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(catalogProcessName);

    return runtime.processInstances.startProcess({
        processDefinitionKey,
        name: options.name,
        businessKey: options.businessKey,
        ...(options.variables ? { variables: options.variables } : {}),
    });
}

export async function startCatalogProcessWithFirstTask(
    runtime: RuntimeBundleService,
    taskService: TaskService,
    catalogProcessName: string,
    options: StartCatalogProcessOptions = {}
): Promise<ProcessWithFirstTask> {
    const processInstance = await startCatalogProcess(runtime, catalogProcessName, options);
    const task = await taskService.getFirstTaskByProcessInstanceId(processInstance.id);
    return { processInstance, task };
}
