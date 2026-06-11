#!/usr/bin/env npx tsx
/**
 * Verifies the preview runtime-bundle exposes the BPMN catalog required by runtime acceptance tests.
 */

import '../config/load-env';
import { RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS, verifyAcceptanceProcessCatalog } from '../helpers/process-deployment';

async function main(): Promise<void> {
    await verifyAcceptanceProcessCatalog();
    console.log(
        `✅ Runtime process catalog OK (${RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS.length} required keys present)`
    );
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
