import { acceptanceLog, acceptancePhase, acceptanceStep } from '../../../helpers/acceptance-progress';
import { runCommandInherit } from './run-shell';

export async function applyClusterPrereqsIfNeeded(): Promise<void> {
    const enabled = process.env.AUTO_CLUSTER_PREREQS?.trim().toLowerCase();
    if (enabled === 'false') {
        return;
    }

    const ns = process.env.PREVIEW_NAME?.trim();
    if (!ns) {
        return;
    }

    acceptancePhase('policies', `Cluster prerequisites — namespace ${ns}`);
    acceptanceStep('policies', 'Handing off to apply-cluster-prereqs.sh (live output below)');
    await runCommandInherit('bash', [
        'activiti-cloud-acceptance-tests-playwright/scripts/apply-cluster-prereqs.sh',
        ns,
    ]);
    acceptanceLog('policies', '✓ Cluster prerequisites applied');
}
