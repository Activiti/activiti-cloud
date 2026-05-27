import { acceptanceLog, acceptancePhase, acceptanceStep } from '../../../helpers/acceptance-progress';
import { REPO_ROOT } from './run-shell';

export async function verifyProcessCatalogIfEnabled(): Promise<void> {
    const enabled = process.env.VERIFY_ACCEPTANCE_PROCESS_CATALOG?.trim().toLowerCase();
    if (enabled === 'false') {
        return;
    }

    acceptancePhase('registry', 'Runtime acceptance process catalog');
    acceptanceStep('registry', 'Checking required BPMN keys on runtime-bundle');
    const { execSync } = await import('child_process');
    execSync('npx tsx activiti-cloud-acceptance-tests-playwright/scripts/verify-acceptance-process-catalog.ts', {
        stdio: 'inherit',
        cwd: REPO_ROOT,
    });
    acceptanceLog('registry', '✓ Process catalog verification passed');
}
