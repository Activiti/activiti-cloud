import { acceptanceLog, acceptancePhase, acceptanceStep } from '../../../helpers/acceptance-progress';
import { verifyAcceptanceProcessCatalog } from '../../../helpers/process-deployment';

export async function verifyProcessCatalogIfEnabled(): Promise<void> {
    const enabled = process.env.VERIFY_ACCEPTANCE_PROCESS_CATALOG?.trim().toLowerCase();
    if (enabled === 'false') {
        return;
    }

    acceptancePhase('registry', 'Runtime acceptance process catalog');
    acceptanceStep('registry', 'Checking required BPMN keys on runtime-bundle');
    await verifyAcceptanceProcessCatalog();
    acceptanceLog('registry', '✓ Process catalog verification passed');
}
