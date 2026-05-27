#!/usr/bin/env npx tsx
/**
 * Verifies the preview runtime-bundle exposes the BPMN catalog required by runtime acceptance tests.
 */

import '../config/load-env';
import { applyResolvedHostsToEnv } from '../config/connection/env-hosts';
import {
    formatMissingProcessCatalogMessage,
    getMissingRequiredProcessDefinitionKeys,
    RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS,
} from '../helpers/process-deployment';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { ContextFactory } from '../context-factory';
applyResolvedHostsToEnv();

async function main(): Promise<void> {
    const context = await ContextFactory.getContextByUserName('testUser');
    const runtimeBundle = new RuntimeBundleService(context);
    const missing = await getMissingRequiredProcessDefinitionKeys(
        runtimeBundle,
        RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS
    );

    if (missing.length > 0) {
        console.error(`\n❌ ${formatMissingProcessCatalogMessage(missing)}\n`);
        process.exit(1);
    }

    console.log(
        `✅ Runtime process catalog OK (${RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS.length} required keys present)`
    );
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
