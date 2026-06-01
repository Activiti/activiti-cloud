/*
 * Start connector/service-task processes (testadmin catalog names).
 */

import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { startCatalogProcess } from './start-catalog-process';

export async function startServiceTaskProcess(
    runtime: RuntimeBundleService,
    catalogProcessName: string,
    variables?: Record<string, unknown>
): Promise<CloudProcessInstance> {
    return startCatalogProcess(runtime, catalogProcessName, variables ? { variables } : {});
}

/** Start by deployed BPMN key (e.g. testErrorConnectorProcess). */
export async function startProcessByDefinitionKey(
    runtime: RuntimeBundleService,
    processDefinitionKey: string,
    variables?: Record<string, unknown>
): Promise<CloudProcessInstance> {
    if (variables) {
        return runtime.startProcessWithVariables(processDefinitionKey, variables);
    }
    return runtime.startProcess({ processDefinitionKey });
}

export function serviceTaskProcessKey(catalogProcessName: string): string {
    return ProcessDefinitionRegistry.getProcessDefinitionKey(catalogProcessName);
}
