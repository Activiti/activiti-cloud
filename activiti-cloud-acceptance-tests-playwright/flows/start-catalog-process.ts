/*
 * Start a process from the acceptance catalog (ProcessDefinitionRegistry name → key).
 */

import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { RuntimeBundleService } from '../services/runtime-bundle/runtime-bundle.service';

export interface StartCatalogProcessOptions {
    name?: string;
    businessKey?: string;
    variables?: Record<string, unknown>;
}

/** Resolve catalog name → deployed process definition key (for assertions on keys). */
export function catalogProcessKey(catalogProcessName: string): string {
    return ProcessDefinitionRegistry.getProcessDefinitionKey(catalogProcessName);
}

export async function startCatalogProcess(
    runtime: RuntimeBundleService,
    catalogProcessName: string,
    options: StartCatalogProcessOptions = {}
): Promise<CloudProcessInstance> {
    const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(catalogProcessName);

    if (options.variables) {
        return runtime.startProcessWithVariables(processDefinitionKey, options.variables, {
            name: options.name,
            businessKey: options.businessKey,
        });
    }

    return runtime.startProcess({
        processDefinitionKey,
        name: options.name,
        businessKey: options.businessKey,
    });
}
