/*
 * Start a catalog process and return the first runtime task (common task-spec setup).
 */

import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudTask } from '../models/task.models';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { TaskService } from '../services/task.service';
import { getFirstProcessTask } from '../helpers/task-assertions';
import { startCatalogProcess, StartCatalogProcessOptions } from './start-catalog-process';

export interface ProcessWithFirstTask {
    processInstance: CloudProcessInstance;
    task: CloudTask;
}

export async function startCatalogProcessWithFirstTask(
    runtime: RuntimeBundleService,
    taskService: TaskService,
    catalogProcessName: string,
    options: StartCatalogProcessOptions = {}
): Promise<ProcessWithFirstTask> {
    const processInstance = await startCatalogProcess(runtime, catalogProcessName, options);
    const task = await getFirstProcessTask(taskService, processInstance.id);
    return { processInstance, task };
}
