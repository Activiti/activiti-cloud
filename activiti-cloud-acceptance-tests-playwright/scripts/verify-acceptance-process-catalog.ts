#!/usr/bin/env npx tsx
/**
 * Verifies the preview runtime-bundle exposes the BPMN catalog required by runtime acceptance tests.
 */

import '../config/load-env';
import { applyResolvedHostsToEnv } from '../config/connection/env-hosts';
import {
    RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS,
    waitForRequiredProcessDefinitions,
} from '../helpers/process-deployment';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { ContextFactory } from '../fixtures/context-factory';

applyResolvedHostsToEnv();

async function main(): Promise<void> {
    const context = await ContextFactory.getContextByUserName('testUser');
    try {
        const runtimeBundle = new RuntimeBundleService(context);
        await waitForRequiredProcessDefinitions(runtimeBundle);

        console.log(
            `✅ Runtime process catalog OK (${RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS.length} required keys present)`
        );
    } finally {
        await context.dispose();
    }
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
